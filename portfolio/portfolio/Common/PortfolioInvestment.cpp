
#include "PortfolioInvestment.h"

namespace cpx {
namespace portfolio {

void Investment::reset()
{
   mId = -1;
   mReturn = std::numeric_limits<double>::quiet_NaN();
   mAllocation = std::numeric_limits<double>::quiet_NaN();
   mName = "";
}

void Investment::onSerialize(soam::OutputStreamPtr &stream) throw (soam::SoamException)
{
   stream->write(mId);
   stream->write(mName);
   stream->write(mReturn);
   stream->write(mAllocation);
}
void Investment::onDeserialize(soam::InputStreamPtr &stream) throw (soam::SoamException)
{
   reset();
   try {
      stream->read(mId);
      stream->read(mName);
      stream->read(mReturn);
      stream->read(mAllocation);
   }
   catch (...) {
      reset();
      throw;
   }
}

void Covariance::reset() { mData.clear(); }

void Covariance::setCovariance(Investment::ID_TYPE i1, Investment::ID_TYPE i2, double covariance)
{
   if ( i1 > i2 )
      std::swap(i1, i2);
   mData.insert(MAP_TYPE::value_type(KEY_TYPE(i1, i2), covariance));
}

double Covariance::getCovariance(Investment::ID_TYPE i1, Investment::ID_TYPE i2) const
{
   if ( i1 > i2 )
      std::swap(i1, i2);
   KEY_TYPE const key(i1, i2);
   MAP_TYPE::const_iterator it(mData.find(key));
   return (it != mData.end()) ? it->second : std::numeric_limits<double>::quiet_NaN();
}

// This is an unsigned type since java has no unsigned types.
typedef long long SIZE_TYPE;

void Covariance::onSerialize(soam::OutputStreamPtr &stream) throw (soam::SoamException)
{
   SIZE_TYPE const size = mData.size();

   stream->write(size);
   for (MAP_TYPE::const_iterator it(mData.begin()); it != mData.end(); ++it) {
      stream->write(it->first.first);
      stream->write(it->first.second);
      stream->write(it->second);
   }
}

void Covariance::onDeserialize(soam::InputStreamPtr &stream) throw (soam::SoamException)
{
   reset();
   try {
      SIZE_TYPE size;
      stream->read(size);
      for (SIZE_TYPE i = 0; i < size; ++i) {
         Investment::ID_TYPE i1, i2;
         double covariance;
         stream->read(i1);
         stream->read(i2);
         stream->read(covariance);
         setCovariance(i1, i2, covariance);
      }
   }
   catch (...) {
      reset();
      throw;
   }
}

} // portfolio
} // cpx
