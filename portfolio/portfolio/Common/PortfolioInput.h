#ifndef CPXSINGLEREQUEST_H
#define CPXSINGLEREQUEST_H 1

#include <soam.h>

#include <vector>

#include "PortfolioInvestment.h"

namespace cpx {
namespace portfolio {

/** Input for the portfolio service.
 * An instance of this messages provides all the data the portfolio service
 * needs to compute an optimal portfolio allocation.
 */
class Input : public soam::Message {
private:
   std::vector<Investment> mInvestments;
   Covariance mCovariance;
   double mWealth;
   double mRho;
   
   Input(Input const &);
   Input &operator=(Input const &);

   void clear();

public:
   Input();
   virtual ~Input();
   void onSerialize(soam::OutputStreamPtr &stream) throw (soam::SoamException);
   void onDeserialize(soam::InputStreamPtr &stream) throw (soam::SoamException);

   // Add/set/get investments from which the service can choose.
   void addInvestment(Investment const &investment) { mInvestments.push_back(investment); }
   void setInvestments(std::vector<Investment> const &investments) { mInvestments = investments; }
   std::vector<Investment> const &getInvestments() const { return mInvestments; }

   // Set/get the covariance matrix that describes the relation of the
   // various investments to each other.
   void setCovariance(Covariance const &covariance) { mCovariance = covariance; }
   Covariance const &getCovariance() const { return mCovariance; }

   // Get/set the initial wealth.
   double getWealth() const { return mWealth; }
   void setWealth(double wealth) { mWealth = wealth; }

   // Get/set the rho factor that determines how much risk the optimal
   // portfolio allocation strategy is allowed to take.
   double getRho() const { return mRho; }
   void setRho(double rho) { mRho = rho; }
};


} // namespace portfolio
} // namespace cpx

#endif // !CPXSINGLEREQUEST_H
