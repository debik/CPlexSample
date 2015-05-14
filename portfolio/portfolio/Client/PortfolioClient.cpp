#include <cmath>
#include <sstream>
#include <cstring>
#include <cstdlib>
#include <fstream>
#include <iostream>

#include <soam.h>

#include "PortfolioInvestment.h"
#include "PortfolioInput.h"
#include "PortfolioOutput.h"

using std::cout;
using std::cerr;
using std::endl;
using namespace soam;
using namespace cpx::portfolio;

/** Hard-coded example data.
 * The data defined here is an example of investments for which the
 * corresponding optimization problem can be solved quickly.
 * If no data file is specified on the command line then the client
 * will use the data defined here.
 */
namespace ExampleData {
   double const returns[20] = {
      1.00125, 1.56359, 1.19330, 1.80874, 1.58501,
      1.47987, 1.35029, 1.89596, 1.82284, 1.74660,
      1.17411, 1.85894, 1.71050, 1.51353, 1.30399,
      1.01498, 1.09140, 1.36445, 1.14731, 1.16590
   };

   double const covariance[20][20] = {
      { 10.95, -0.119083, -0.0089114, 0.531663, 0.601764, 0.166234, -0.450789, 0.0570391, 0.783319, -0.519883, 0.875973, 0.955901, -0.539354, -0.462081, -0.862239, 0.779656, 0.996796, -0.611499, -0.266213, -0.840144},
      { -0.119083, 9.81777, -0.677206, 0.00878933, -0.275887, 0.587909, 0.837611, -0.484939, -0.743736, 0.457961, -0.744438, -0.599048, 0.735008, -0.572405, -0.151555, 0.425153, 0.517106, -0.751549, 0.168981, -0.491897},
      { -0.0089114, -0.677206, 9.54527, 0.147496, 0.141575, -0.692892, -0.426557, 0.966613, 0.153233, 0.82168, -0.191351, -0.817194, 0.155553, -0.732017, 0.27958, 0.682241, -0.721915, -0.12302, -0.834681, 0.517014},
      { 0.531663, 0.00878933, 0.147496, 11.229, 0.949339, -0.549547, -0.471725, -0.84698, -0.456099, -0.982971, 0.739189, 0.19599, -0.839442, 0.5009, 0.0274667, -0.572588, -0.531327, 0.843043, -0.657613, -0.842158},
      { 0.601764, -0.275887, 0.141575, 0.949339, 8.9361, 0.314066, -0.286081, 0.140263, 0.83462, 0.600238, -0.252724, -0.00161748, 0.806238, -0.210578, -0.553209, -0.113773, 0.75222, -0.543443, -0.43672, -0.696219},
      { 0.166234, 0.587909, -0.692892, -0.549547, 0.314066, 9.26831, 0.577868, -0.628681, 0.504135, 0.695761, -0.189947, 0.17835, 0.457442, 0.0975066, -0.0943938, -0.931516, -0.89462, 0.227302, -0.410718, 0.628071},
      { -0.450789, 0.837611, -0.426557, -0.471725, -0.286081, 0.577868, 11.0518, 0.597827, 0.854793, 0.624775, -0.565752, 0.184271, 0.555132, -0.242866, 0.604724, -0.584613, -0.494461, 0.740745, 0.62038, -0.804529},
      { 0.0570391, -0.484939, 0.966613, -0.84698, 0.140263, -0.628681, 0.597827, 9.91122, 0.911557, -0.727683, 0.667776, 0.315012, -0.305826, 0.108554, 0.851222, -0.154881, -0.0793481, 0.64098, -0.545091, -0.408979},
      { 0.783319, -0.743736, 0.153233, -0.456099, 0.83462, 0.504135, 0.854793, 0.911557, 11.0092, -0.152654, -0.737999, 0.826685, 0.873348, 0.300058, -0.127232, 0.784967, 0.609638, 0.0722678, -0.653859, 0.104801},
      { -0.519883, 0.457961, 0.82168, -0.982971, 0.600238, 0.695761, 0.624775, -0.727683, -0.152654, 11.4757, 0.91998, -0.662801, -0.492538, 0.496811, -0.509262, 0.688162, -0.606281, 0.00589007, 0.10062, -0.863247},
      { 0.875973, -0.744438, -0.191351, 0.739189, -0.252724, -0.189947, -0.565752, 0.667776, -0.737999, 0.91998, 9.71151, 0.380108, -0.552812, 0.955718, -0.17658, 0.131626, 0.95172, -0.0278329, 0.0559099, 0.131626},
      { 0.955901, -0.599048, -0.817194, 0.19599, -0.00161748, 0.17835, 0.184271, 0.315012, 0.826685, -0.662801, 0.380108, 8.88156, 0.721641, -0.0146794, 0.707907, -0.217566, 0.16892, -0.340983, 0.367565, 0.802393},
      { -0.539354, 0.735008, 0.155553, -0.839442, 0.806238, 0.457442, 0.555132, -0.305826, 0.873348, -0.492538, -0.552812, 0.721641, 10.189, 0.798181, 0.14481, -0.402417, 0.13654, -0.0661641, -0.573351, -0.548051},
      { -0.462081, -0.572405, -0.732017, 0.5009, -0.210578, 0.0975066, -0.242866, 0.108554, 0.300058, 0.496811, 0.955718, -0.0146794, 0.798181, 9.25395, -0.808039, 0.284249, 0.89523, 0.743797, 0.361126, 0.228492},
      { -0.862239, -0.151555, 0.27958, 0.0274667, -0.553209, -0.0943938, 0.604724, 0.851222, -0.127232, -0.509262, -0.17658, 0.707907, 0.14481, -0.808039, 8.40558, 0.542405, -0.0538041, -0.524674, -0.0946684, -0.891537},
      { 0.779656, 0.425153, 0.682241, -0.572588, -0.113773, -0.931516, -0.584613, -0.154881, 0.784967, 0.688162, 0.131626, -0.217566, -0.402417, 0.284249, 0.542405, 9.46003, -0.931639, -0.0470901, 0.336406, -0.398602},
      { 0.996796, 0.517106, -0.721915, -0.531327, 0.75222, -0.89462, -0.494461, -0.0793481, 0.609638, -0.606281, 0.95172, 0.16892, 0.13654, 0.89523, -0.0538041, -0.931639, 11.3699, 0.534227, -0.693533, -0.259163},
      { -0.611499, -0.751549, -0.12302, 0.843043, -0.543443, 0.227302, 0.740745, 0.64098, 0.0722678, 0.00589007, -0.0278329, -0.340983, -0.0661641, 0.743797, -0.524674, -0.0470901, 0.534227, 8.2202, -0.398694, -0.585559},
      { -0.266213, 0.168981, -0.834681, -0.657613, -0.43672, -0.410718, 0.62038, -0.545091, -0.653859, 0.10062, 0.0559099, 0.367565, -0.573351, 0.361126, -0.0946684, 0.336406, -0.693533, -0.398694, 8.41221, 0.435499},
      { -0.840144, -0.491897, 0.517014, -0.842158, -0.696219, 0.628071, -0.804529, -0.408979, 0.104801, -0.863247, 0.131626, 0.802393, -0.548051, 0.228492, -0.891537, -0.398602, -0.259163, -0.585559, 0.435499, 11.0019}
   };

   double const defaultWealth = 100;
   double const defaultRho = 0.01;

   /** Populate a task input message with he hard-coded example data.
    * If either of wealth or rho is NaN then the default values defined in
    * this namespace will be used.
    */
   void populate(Input &input, double &wealth, double &rho) {
      std::vector<Investment> investments;
      Covariance covar;
      for (int i = 0; i < 20; ++i) {
         std::stringstream s;
         s << "Investment" << i;
         investments.push_back(Investment(i, s.str(), returns[i]));
      }
      for (int i = 0; i < 20; ++i)
         for (int j = 0; j < 20; ++j)
            covar.setCovariance(i, j, covariance[i][j]);
      if ( isnan(wealth) )
         wealth = defaultWealth;
      if ( isnan(rho) )
         rho = defaultRho;
      input.setInvestments(investments);
      input.setCovariance(covar);
      input.setWealth(wealth);
      input.setRho(rho);
   }

} // namespace ExampleData

int
main(int argc, char* argv[])
{
   int returnValue = 0;

   int nuOfTasks = 0;
   bool waitResult = true;
   char const *sessionId = 0;
   SoamLong timeout = globalConst(TimeoutInfinite);
   char const *dataFile = 0;
   double wealth = std::numeric_limits<double>::quiet_NaN();
   double step = std::numeric_limits<double>::quiet_NaN();
   double rhomin = std::numeric_limits<double>::quiet_NaN();
   double rhomax = std::numeric_limits<double>::quiet_NaN();

   // Parse the command line.
   for (int i = 1; i < argc; ++i) {
      if ( ::strcmp(argv[i], "-wait") == 0 )
         waitResult = true;
      else if ( ::strcmp(argv[i], "-no-wait") == 0 )
         waitResult = false;
      else if ( ::strncmp(argv[i], "-session=", 9) == 0 )
         sessionId = argv[i] + 9;
      else if ( ::strncmp(argv[i], "-timeout=", 9) == 0 )
         timeout = ::strtol(argv[i] + 9, 0, 10);
      else if ( ::strncmp(argv[i], "-data=", 6) == 0 )
         dataFile = argv[i] + 6;
      else if ( ::strncmp(argv[i], "-wealth=", 8) == 0 )
         wealth = ::strtod(argv[i] + 8, 0);
      else if ( ::strncmp(argv[i], "-step=", 6) == 0 )
         step = ::strtod(argv[i] + 6, 0);
      else if ( ::strncmp(argv[i], "-rho=", 5) == 0 ) {
         char * pch;

         pch = ::strtok(argv[i] + 5, ",");
         if (pch != NULL) {
            rhomin = strtod(pch,0);
          
            pch = :: strtok(NULL, ",");
            if (pch != NULL ) {
               rhomax = strtod(pch, 0);
            }
         }
      }
      else if ( ::strcmp(argv[i], "-help") != 0 ) {
         cerr << "Unknown argument " << argv[i] << endl;
         return -1;
      }
      else {
         cout
            << "Command line client for portfolio optimization service." << endl
            << "Usage: " << argv[0] << " [options]" << endl
            << "  By default the client will submit a new portfolio" << endl
            << "  optimization problem to the service and wait for" << endl
            << "  the results." << endl
            << "  [options] can be any combination of the following:" << endl
            << "    -wait         Wait until the service reports the" << endl
            << "                  optimal allocation and then print the" << endl
            << "                  optimal allocation." << endl
            << "    -no-wait      Do not wait for the service to complete," << endl
            << "                  instead detach immediately. The client" << endl
            << "                  will print the session id that can be" << endl
            << "                  used as argument to -session to attach" << endl
            << "                  to the session later." << endl
            << "    -session=<id> Do not submit a new problem. Instead" << endl
            << "                  attach to session <id> that was started" << endl
            << "                  in a previous run of the the client." << endl
            << "    -data=<file>  Instead of using the hard-coded example"<< endl
            << "                  data read investments and covariance" << endl
            << "                  matrix from <file>. This requires -rho" << endl
            << "                  and -wealth." << endl
            << "    -rho=...      Specify the risk factor to be used in" << endl
            << "                  optimization. Can be given as a single value " << endl
            << "                  or a range between 0 and 1, for example -rho=0,1" << endl
            << "    -wealth=...   Specify the initial wealth to be used in" << endl
            << "                  optimization." << endl
            << "    -step=...     If rho is given as a range, specify the step width." << endl
            ;
         return 0;
      }
   }

   // Make sure we have all data we need.
   if ( dataFile ) {
      if (isnan(wealth) || isnan(rhomin)) {
         cerr << "Specifying -data= also requires -wealth= and -rho=" << endl;
         return -1;
      }
      if ( !((isnan(rhomax) && isnan(step)) || 
           (!(isnan(rhomax) || isnan(step)) && rhomax>rhomin && rhomax > 0 && step > 0)) ) {
         cerr << "Invalid value for rho and/or step. Rho has to be a single value, or a range" << endl
              << "of values with <rhomin,rhomax> separated by ',' and 0 <= rhomin < rhomax." << endl
              << "Step has to be > 0." << endl;
         return -2;
      }
   }

   // Now start the client.
   try
   {
      /********************************************************************
       * We should initialize the API before using any API calls. 
       ********************************************************************/
      SoamFactory::initialize();

      // Set up application specific information to be supplied to
      // Symphony. 
      char appName[] = "PortfolioClient";

      // Set up application authentication information using the default
      // security provider. Ensure it exists for the lifetime of the
      // connection. 
      DefaultSecurityCallback securityCB("Admin", "Admin");

      // Connect to the specified application. 
      ConnectionPtr conPtr = SoamFactory::connect(appName, &securityCB);

      // Retrieve and print our connection ID. 
      cout << "connection ID=" << conPtr->getId() << endl; 

      // Create a new session or connect to an existing session.
      SessionPtr sesPtr;
      if ( sessionId ) {
         // A session id was specified on the command line. Just
         // attach to that session.
         SessionOpenAttributes attributes;
         attributes.setSessionId(sessionId);
         attributes.setSessionName("mySession");
         attributes.setSessionFlags(Session::ReceiveSync);
         sesPtr = conPtr->openSession(attributes);
      }
      else {
         // No session id specified on the command line. Create a new
         // session and submit an optimmization problem to it.
         SessionCreationAttributes attributes;
         attributes.setSessionName("mySession");
         attributes.setSessionType("ShortRunningTasks");
         attributes.setSessionFlags(Session::ReceiveSync);
         sesPtr = conPtr->createSession(attributes);

         // Retrieve and print session ID. 
         cout << "Session ID:" << sesPtr->getId() << endl;

         // Prepare the input message for the service.
         Input input;
         if ( dataFile ) {
            std::vector<Investment> investments;
            Covariance covar;
            std::ifstream file(dataFile);
            load(file, std::back_inserter(investments), covar);
            // Check data integrity.
            for (std::vector<Investment>::const_iterator it = investments.begin(); it != investments.end(); ++it) {
               for (std::vector<Investment>::const_iterator jt = it; jt != investments.end(); ++jt) {
                  if ( isnan(covar.getCovariance(it->getId(), jt->getId())) ) {
                     std::cerr << "No covariance for " << it->getName() << " and " << jt->getName() << std::endl;
                     throw soam::FatalException("Data error");
                  }
               }
            }
            input.setInvestments(investments);
            input.setCovariance(covar);
            input.setWealth(wealth);
            input.setRho(rhomin);
         }
         else {
            ExampleData::populate(input, wealth, rhomin);
         }

         // Setting benign values in case rho was specified as single value.
         // This will result in one task for rho=rhomin
         if ( isnan(rhomax) || isnan(step)) {
            rhomax = rhomin;
            step = 1;
         }

         // Submit the input message to start optimization.
         TaskSubmissionAttributes attrTask;
         TaskInputHandlePtr inputHandle = 0;

         for (double d=rhomin; d <= rhomax; d+=step) {
            input.setRho(d);
            attrTask.setTaskInput(&input);
            inputHandle = sesPtr->sendTaskInput(attrTask);
            nuOfTasks++;
            
         // Retrieve and print task ID. 
         cout << "task submitted with ID : " << inputHandle->getId() << endl;
         }
         cout << "Number of tasks submitted: " << nuOfTasks << endl;
      }

      if ( waitResult ) {
         // Now get our results - will block here until all tasks retrieved. 
         EnumItemsPtr enumOutput = sesPtr->fetchTaskOutput(nuOfTasks, timeout);

         if ( enumOutput->getCount() != nuOfTasks ) {
            // No output yet
            cout << "Task not complete yet (timeout=" << timeout << " s),"
                 << " try again later" << endl;
            sesPtr->close(Session::DetachOnClose);
         }
         else {
            // Task is complete. Display results
            TaskOutputHandlePtr outputHandle;
            while(enumOutput->getNext(outputHandle)) {
               // Check for success of task. 
               if ( true == outputHandle->isSuccessful() ) {
                  // Get the message returned from the service. 
                  Output output;
                  outputHandle->populateTaskOutput(&output);

                  cout << "Allocation plan for wealth " << output.getWealth()
                       << " and rho " << output.getRho() << ":" << endl;
                  typedef std::vector<Investment> VECTOR_TYPE;
                  VECTOR_TYPE const &investments = output.getInvestments();
                  for (VECTOR_TYPE::const_iterator it(investments.begin());
                       it != investments.end(); ++it)
                     cout << it->getId() << ", " << it->getName() << ": "
                          << it->getAllocation() << endl;
                  cout << "Total return = " << output.getTotalReturn() << endl;
                  cout << "Total variance = " << output.getTotalVariance() << endl;
               }
               else {
                  // Get the exception associated with this task. 
                  SoamExceptionPtr ex = outputHandle->getException();
                  cout << "Task Not Succeeded : " << endl
                       << "Error code: " << ex->getErrorCode() << endl
                       << "Error message: " << ex->what() << endl;
                  returnValue = -1;
               }
            }

            sesPtr->close();
            conPtr->close();
         }
      }
      else {
         // The -no-wait argument was specified. We detach from the
         // session and exit.
         cout << "Detaching. Use argument -session=" << sesPtr->getId()
              << " to reconnect." << endl;
         sesPtr->close(Session::DetachOnClose);
         conPtr->close();
      }
   }
   catch(SoamException& exp)
   {
      // Report exception. 
      cout << "exception caught ... " << exp.what() << endl;
      returnValue = -1;
   }
   catch(ioutil::Exception& exp)
   {
      // Report exception. 
      cout << "exception caught ... " << exp.what() << endl;
      returnValue = -1;
   }

   /************************************************************************
    * It is important that we always uninitialize the API. This is the only
    * way to ensure proper shutdown of the interaction between the client
    * and the system. 
    ************************************************************************/
   SoamFactory::uninitialize();

   return returnValue;
}

