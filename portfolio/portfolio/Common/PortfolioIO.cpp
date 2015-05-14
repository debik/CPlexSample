
#include "PortfolioInput.h"
#include "PortfolioOutput.h"

namespace cpx {
namespace portfolio {

// Since Java has no unsigned types and we want to be able to exchange
// data with Java clients, this is a signed and not an unsigned type.
typedef long long SIZE_TYPE;

Input::Input()
   : mInvestments()
   , mCovariance()
   , mWealth(std::numeric_limits<double>::quiet_NaN())
   , mRho(std::numeric_limits<double>::quiet_NaN())
{
}

Input::~Input()
{
}

void Input::clear()
{
   mCovariance.clear();
   mInvestments.clear();
   mWealth = std::numeric_limits<double>::quiet_NaN();
   mRho = std::numeric_limits<double>::quiet_NaN();
}

void Input::onSerialize(soam::OutputStreamPtr &stream) throw (soam::SoamException)
{
   SIZE_TYPE const size = mInvestments.size();
   stream->write(size);
   for (SIZE_TYPE i = 0; i < size; ++i)
      mInvestments[i].onSerialize(stream);
   mCovariance.onSerialize(stream);
   stream->write(mWealth);
   stream->write(mRho);
}

void Input::onDeserialize(soam::InputStreamPtr &stream) throw (soam::SoamException)
{
   clear();
   try {
      SIZE_TYPE size;
      stream->read(size);
      for (SIZE_TYPE i = 0; i < size; ++i) {
         Investment investment;
         investment.onDeserialize(stream);
         addInvestment(investment);
      }
      mCovariance.onDeserialize(stream);
      stream->read(mWealth);
      stream->read(mRho);
   }
   catch (...) {
      clear();
      throw;
   }
}


Output::Output()
   : mOptimal(false)
   , mWealth(std::numeric_limits<double>::quiet_NaN())
   , mRho(std::numeric_limits<double>::quiet_NaN())
   , mObjValue(std::numeric_limits<double>::quiet_NaN())
   , mTotalReturn(std::numeric_limits<double>::quiet_NaN())
   , mTotalVariance(std::numeric_limits<double>::quiet_NaN())
   , mInvestments()
{
}

Output::~Output()
{
}

void Output::clear()
{
   mOptimal = false;
   mWealth = std::numeric_limits<double>::quiet_NaN();
   mRho = std::numeric_limits<double>::quiet_NaN();
   mObjValue = std::numeric_limits<double>::quiet_NaN();
   mTotalReturn = std::numeric_limits<double>::quiet_NaN();
   mTotalVariance = std::numeric_limits<double>::quiet_NaN();
   mInvestments.clear();
}

void Output::onSerialize(soam::OutputStreamPtr &stream) throw (soam::SoamException)
{
   stream->write(mOptimal);
   stream->write(mWealth);
   stream->write(mRho);
   stream->write(mObjValue);
   stream->write(mTotalReturn);
   stream->write(mTotalVariance);
   SIZE_TYPE const size = mInvestments.size();
   stream->write(size);
   for (SIZE_TYPE i = 0; i < size; ++i)
      mInvestments[i].onSerialize(stream);
}

void Output::onDeserialize(soam::InputStreamPtr &stream) throw (soam::SoamException)
{
   clear();
   try {
      stream->read(mOptimal);
      stream->read(mWealth);
      stream->read(mRho);
      stream->read(mObjValue);
      stream->read(mTotalReturn);
      stream->read(mTotalVariance);
      SIZE_TYPE size;
      stream->read(size);
      for (SIZE_TYPE i = 0; i < size; ++i) {
         mInvestments.push_back(Investment());
         mInvestments[i].onDeserialize(stream);
      }
   }
   catch (...) {
      clear();
      throw;
   }
}

} // namespace portfolio
} // namespace cpx
