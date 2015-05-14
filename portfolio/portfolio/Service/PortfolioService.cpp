// Portfolio Optimization
//
// This OPL model is a tool for investment portfolio optimization. The
// model is formulated as a Quadratic Programming (QP) problem. A complete
// description of the theory of portfolio investment that underlies this
// formulation can be found in the book:
//
// Portfolio Selection: Efficient Diversification of Investments by
// Harry M. Markowitz, see for example http://cowles.econ.yale.edu/P/cm/m16/
//
// The model requires:
//
// - a set of investment options with expected returns
// - a positive semi-definite covariance matrix describing the
//   dependencies between all investment options
// - a user defined parameter indicating the preferred trade-off between
//   risk and reward (called "rho")

#include <iostream>
#include <soam.h>
#include <ilcplex/ilocplex.h>

#include <sstream>

#include "PortfolioInput.h"
#include "PortfolioOutput.h"

namespace cpx {
namespace portfolio {

   using namespace soam;

   /** Wrapper around an IloEnv instance that makes sure the instance is
    * end()'d when it leaves the scope.
    */
   struct ScopedEnv {
      IloEnv env;
      ScopedEnv() : env() {}
      ~ScopedEnv() { env.end(); }
   };

   /** A stream buffer to capture CPLEX log output.
    */
   struct LogBuffer : public std::streambuf {

      std::string buffer;

      void add(char c) {
         if ( c == '\n' ) {
            // In this simple implementation we print the log to standard
            // output. In a more sophisticated implementation we could
            // report it back to the client.
            std::cout << buffer << std::endl;
            buffer = "";
         }
         else
            buffer.push_back(c);
      }

      // Override super class methods.
      int overflow(int c = EOF) {
         if ( c != EOF )
            add(c);
         return c;
      }
      std::streamsize xsputn (char const *s, std::streamsize n) {
         for (std::streamsize i = 0; i < n; ++i)
            add(s[i]);
         return n;
      }
   };

   /** The actual service implementation.
    */
   class Service : public ServiceContainer {
   public:
      virtual void onInvoke(TaskContextPtr& taskContext)
      {
         /********************************************************************
          * Do your service logic here. This call applies to each task
          * submission. 
          ********************************************************************/
         ScopedEnv envScope; // So that we cleanup on exit or exception
         IloEnv &env = envScope.env;

         typedef std::vector<Investment> VECTOR_TYPE;
         VECTOR_TYPE investments;

         try {
            IloModel model(env);
            IloNumVarArray allocation(env);

            IloExpr totalReturn(env), totalVariance(env);
            double wealth, rho;

            // Extract the input arguments from the message and setup
            // the optimization problem in CPLEX.
            // The optimization problem is the following (in OPL notation),
            // which can also be found in the portfolio.mod file in the CPLEX
            // distribution.
            //   {string} Investments = ...;
            //   float Return[Investments] = ...;
            //   float Covariance[Investments][Investments] = ...;
            //   float Wealth = ...;
            //   float Rho = ...;  // Variance Penalty (increasing rho from 0.001 to 1.0 
            //                     //                   produces a distribution of funds 
            //                     //                   with smaller and smaller variability).
            //
            //   /******************************************************************************
            //    * MODEL DECLARATIONS
            //    ******************************************************************************/
            //
            //   range float FloatRange = 0.0..Wealth;
            //
            //   dvar float  Allocation[Investments] in FloatRange;  // Investment Level
            //
            //   /******************************************************************************
            //    * MODEL
            //    ******************************************************************************/
            //
            //   dexpr float Objective =
            //     (sum(i in Investments) Return[i]*Allocation[i])
            //       - (Rho/2)*(sum(i,j in Investments) Covariance[i][j]*Allocation[i]*Allocation[j]);
            //
            //   maximize Objective;
            //
            //   subject to {
            //     // sum of allocations equals amount to be invested
            //     allocate: (sum (i in Investments) (Allocation[i])) == Wealth;
            //   }
            //
            //   float TotalReturn = sum(i in Investments) Return[i]*Allocation[i];
            //   float TotalVariance = sum(i,j in Investments) Covariance[i][j]*Allocation[i]*Allocation[j];
            //
            // Note that instead of having a separate array for investment
            // name and return, we just have an array of investment objects that
            // each have a name and return property.
            // The objective function in the model balances expected return
            // versus total variance. The balancing factor is rho.
            { // Extra block to limit scope of potentially large input message
               Input input;
               taskContext->populateTaskInput(input);
               Covariance const &covariance = input.getCovariance();
               wealth = input.getWealth();
               rho = input.getRho();
               investments = input.getInvestments();

               // range float FloatRange = 0.0..Wealth;
               // dvar float  Allocation[Investments] in FloatRange;
               for (VECTOR_TYPE::size_type i = 0; i < investments.size(); ++i) {
                  std::stringstream name;
                  name << investments[i].getName()
                       << " (" << investments[i].getId() << ")";
                  allocation.add(IloNumVar(env, 0.0, wealth, name.str().c_str()));
               }

               // dexpr float Objective =
               //  (sum(i in Investments) Return[i]*Allocation[i])
               //    - (Rho/2)*(sum(i,j in Investments) Covariance[i][j]*Allocation[i]*Allocation[j]);
               // maximize Objective;
               IloExpr objective(env);
               for (VECTOR_TYPE::size_type i = 0; i < investments.size(); ++i)
                  objective += investments[i].getReturn() * allocation[i];
               for (VECTOR_TYPE::size_type i = 0; i < investments.size(); ++i) {
                  for (VECTOR_TYPE::size_type j = 0; j < investments.size(); ++j) {
                     double const cov = covariance.getCovariance(investments[i].getId(),
                                                                 investments[j].getId());
                     objective -= 0.5 * rho * cov * allocation[i] * allocation[j];
                  }
               }
               model.add(IloMaximize(env, objective));
               objective.end();

               // allocate: (sum (i in Investments) (Allocation[i])) == Wealth;
               IloExpr sum(env);
               for (VECTOR_TYPE::size_type i = 0; i < investments.size(); ++i)
                  sum += allocation[i];
               model.add(IloRange(env, input.getWealth(), sum, input.getWealth(),
                                  "allocation"));

               // float TotalReturn = sum(i in Investments) Return[i]*Allocation[i];
               // float TotalVariance = sum(i,j in Investments) Covariance[i][j]*Allocation[i]*Allocation[j];
               for (VECTOR_TYPE::size_type i = 0; i < investments.size(); ++i)
                  totalReturn += investments[i].getReturn() * allocation[i];
               for (VECTOR_TYPE::size_type i = 0; i < investments.size(); ++i) {
                  for (VECTOR_TYPE::size_type j = 0; j < investments.size(); ++j) {
                     double const cov = covariance.getCovariance(investments[i].getId(),
                                                                 investments[j].getId());
                     totalVariance += cov * allocation[i] * allocation[j];
                  }
               }

               taskContext->discardInputMessage();
            }

            // Create a CPLEX instance and solve the optimization problem.
            IloCplex cplex(env);
            LogBuffer logBuffer;
            std::ostream log(&logBuffer);
            cplex.setOut(log);
            cplex.extract(model);
            bool const feasible = cplex.solve();
            log << "\n"; // Flush the buffer (CPLEX _always_ uses "\n")

            // Setup the service output.
            Output output;
            output.setWealth(wealth);
            output.setRho(rho);
            if ( feasible ) {
               // Found a feasible solution.
               output.setObjValue(cplex.getObjValue());
               output.setTotalReturn(cplex.getValue(totalReturn));
               output.setTotalVariance(cplex.getValue(totalVariance));
               IloNumArray vals(env);
               cplex.getValues(allocation, vals);
               for (VECTOR_TYPE::size_type i = 0; i < investments.size(); ++i)
                  investments[i].setAllocation(vals[i]);
               output.setInvestments(investments);
               output.setOptimal(true);
            }
            else {
               // No feasible solution found.
            }
            taskContext->setTaskOutput(output);
         }
         catch (SoamException const &) {
            throw;
         }
         catch (IloAlgorithm::CannotExtractException e) {
            // This exception gets special treatment because it usually
            // points to errors in the input data.
            std::stringstream s;
            s << e.getMessage() << std::endl;
            IloExtractableArray &a = e.getExtractables();
            for (IloInt i = 0; i < a.getSize(); ++i)
               s << a[i] << std::endl;
            throw soam::FatalException(s.str().c_str());
         }
         catch (IloException const &e) {
            throw soam::FatalException(e.getMessage());
         }
         catch (std::exception const &e) {
            throw soam::FatalException(e.what());
         }
         catch (...) {
            throw soam::FatalException("Unknown exception caught");
         }
      }
   };

} // namespace portfolio
} // namespace cpx


// Entry point to the service 
int main(int argc, char* argv[])
{
   // Return value of our service program 
   int returnValue = 0;

   try
   {
      /********************************************************************
       * Do not implement any service initialization before calling the
       * ServiceContainer::run() method. If any service initialization
       * needs to be done, implement the onCreateService() handler for your
       * service container. 
       ********************************************************************/
        
      // Create the container and run it 
      cpx::portfolio::Service().run();
        
      /********************************************************************
       * Do not implement any service uninitialization after calling the
       * ServiceContainer::run() method. If any service uninitialization
       * needs to be done, implement the onDestroyService() handler for
       * your service container since there is no guarantee that the
       * remaining code in main() will be executed after calling
       * ServiceContainer::run(). Also, in some cases, the remaining code
       * can even cause an orphan service instance if the code cannot be
       * finished. 
       ********************************************************************/
   }
   catch(soam::SoamException& exp) {
      // Report the exception to stdout 
      std::cout << "exception caught ... " << exp.what() << std::endl;
      returnValue = -1;
   }

   /************************************************************************
    * NOTE: Although our service program will return an overall failure or
    * success code it will always be ignored in the current revision of the
    * middleware. The value being returned here is for consistency. 
    ************************************************************************/
   return returnValue;
} 
