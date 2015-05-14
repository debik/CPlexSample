#ifndef CPXSINGLEREPLY_H
#define CPXSINGLEREPLY_H 1

#include <soam.h>
#include <vector>

#include "PortfolioInvestment.h"

namespace cpx {
namespace portfolio {

/** Portfolio service output.
 * An instance of this class represents the output of the portfolio service.
 * It either specifies an optimal portfolio allocation or indicates that no
 * feasible allocation was found. In the latter case isOptimal() will
 * return false and functions getTotalReturn() and 
 * getTotalVariance() will both return Double#NaN.
 */
class Output : public soam::Message {
   Output(Output const &);
   Output &operator=(Output const &);

   bool mOptimal;
   double mWealth;
   double mRho;
   double mObjValue;
   double mTotalReturn;
   double mTotalVariance;
   std::vector<Investment> mInvestments;

   void clear();
public:
   Output();
   virtual ~Output();
   void onSerialize(soam::OutputStreamPtr &stream) throw (soam::SoamException);
   void onDeserialize(soam::InputStreamPtr &stream) throw (soam::SoamException);

   // Test/set whether this instance represents an optimal solution.
   bool isOptimal() const { return mOptimal; }
   void setOptimal(bool optimal) { mOptimal = optimal; }

   // Get/set the initial wealth for which this instance was computed.
   double getWealth() const { return mWealth; }
   void setWealth(double wealth) { mWealth = wealth; }

   // Get/set the rho value for which this instance was computed.
   double getRho() const { return mRho; }
   void setRho(double rho) { mRho = rho; }

   // Get/set the objective value of the optimization problem that was solved.
   double getObjValue() const { return mObjValue; }
   void setObjValue(double objValue) { mObjValue = objValue; }

   // Get/set the total return of the optimal portfolio allocation.
   double getTotalReturn() const { return mTotalReturn; }
   void setTotalReturn(double totalReturn) { mTotalReturn = totalReturn; }

   // Get/set the total variance of the optimal portfolio allocation.
   double getTotalVariance() const { return mTotalVariance; }
   void setTotalVariance(double totalVariance) { mTotalVariance = totalVariance; }

   // Get/set the optimal portfolio allocation.
   std::vector<Investment> const &getInvestments() const { return mInvestments; }
   void setInvestments(std::vector<Investment> const &investments) { mInvestments = investments; }
};

} // namespace portfolio
} // namespace cpx

#endif // !CPXSINGLEREPLY_H
