#ifndef CPXPORTFOLIOINVESTMENT_H
#define CPXPORTFOLIOINVESTMENT_H 1

#include <soam.h>

#include <map>
#include <string>
#include <limits>
#include <cctype>
#include <vector>
#include <sstream>
#include <iostream>

namespace cpx {
namespace portfolio {

/** Investment descriptor.
 * Instances of this class describe an investment in the input and the
 * output of the service. When used as input the service then the allocation
 * is usually NaN. When used as output from the service the
 * allocation specifies the amount of this investment in the optimal portfolio
 * allocation.
 * Each investment has a unique id that identifies it. The name of an
 * investment is only used for display.
 */
struct Investment {
   typedef long long ID_TYPE;
private:
   /** Unique id for this investment. */
   ID_TYPE mId;
   /** Display name for this investment. */
   std::string mName;
   /** Expected return for this investment. */
   double mReturn;
   /** Optimal allocation for this investment. */
   double mAllocation;

   void reset();
public:
   Investment(ID_TYPE id = -1,
              std::string const &name = "",
              double ret = std::numeric_limits<double>::quiet_NaN(),
              double allocation = std::numeric_limits<double>::quiet_NaN())
      : mId(id), mName(name), mReturn(ret), mAllocation(allocation)
   {}

   ID_TYPE getId() const { return mId; }
   void setId(ID_TYPE id) { mId = id; }

   std::string const &getName() const { return mName; }
   void setName(std::string const &name) { mName = name; }

   double getReturn() const { return mReturn; }
   void setReturn(double const &ret) { mReturn = ret; }

   double getAllocation() const { return mAllocation; }
   void setAllocation(double const &allocation) { mAllocation = allocation; }
   
   virtual void onSerialize(soam::OutputStreamPtr &stream) throw (soam::SoamException);
   virtual void onDeserialize(soam::InputStreamPtr &stream) throw (soam::SoamException);
};

/** Covariance descriptor.
 * Instances of this class are used as input to the portfolio optimization
 * service. This class represents a covariance matrix my means of a map that
 * is indexed by pairs of investment ids.
 */
class Covariance {
   typedef std::pair<Investment::ID_TYPE, Investment::ID_TYPE> KEY_TYPE;
   typedef std::map<KEY_TYPE, double> MAP_TYPE;

   MAP_TYPE mData;

   void reset();
public:

   Covariance() : mData() {}

   void clear() { mData.clear(); }

   // Set/get covariance for a pair of investments.
   // The investments to query are given by their id. The order does not
   // matter since a covariance matrix is symmetric.
   // If no covariance is stored for the specified pair the get-function
   // returns NaN.
   void setCovariance(Investment::ID_TYPE i1, Investment::ID_TYPE i2, double covariance);
   double getCovariance(Investment::ID_TYPE i1, Investment::ID_TYPE i2) const;

   virtual void onSerialize(soam::OutputStreamPtr &stream) throw (soam::SoamException);
   virtual void onDeserialize(soam::InputStreamPtr &stream) throw (soam::SoamException);
};

/** Utilities for functions that read and write data. */
namespace ioutil {
   struct Triple {
      Investment::ID_TYPE id1;
      Investment::ID_TYPE id2;
      double covariance;
      Triple(Investment::ID_TYPE const &i1, Investment::ID_TYPE const &i2,
             double const &cov)
         : id1(i1), id2(i2), covariance(cov) {}
   };

   class Exception : public std::exception {
      char const *message;
   public:
      Exception(char const *msg) throw()
         : std::exception(), message(msg) {}
      Exception(Exception const &e) throw()
         : std::exception(e), message(e.message) {}
      Exception &operator=(Exception const &e) throw() {
         std::exception::operator=(e);
         message = e.message;
         return *this;
      }
      ~Exception() throw() {}
      char const *what() const throw() { return message; }
   };
}


/** Save investments and covariance matrix to a file.
 * @param file        The file to which the function saves.
 * @param investments A container (with begin(), end(), and const_iterator
 *                    members) that specifies the investments to save.
 * @param covariance  The covariance matrix to save.
 */
template<typename I>
void save(std::ostream &file, I const &investments, Covariance const &covariance)
{
   if ( !file )
      throw ioutil::Exception("Output error");
   for (typename I::const_iterator it = investments.begin();
        it != investments.end(); ++it)
   {
      file << "I " << it->getId() << " " << it->getReturn()
           << " " << it->getName() << std::endl;
      if ( !file )
         throw ioutil::Exception("Output error");
      for (typename I::const_iterator jt = it; jt != investments.end(); ++jt) {
         file << "C " << it->getId() << " " << jt->getId()
              << " " << covariance.getCovariance(it->getId(), jt->getId())
              << std::endl;
         if ( !file )
            throw ioutil::Exception("Output error");
      }
   }
}

template<typename I>
void load(std::istream &file, I investments, Covariance &covariance)
{
   covariance.clear();
   int lineno = 0;
   std::map<Investment::ID_TYPE, Investment> is;
   std::vector<ioutil::Triple> triples;

   while (file) {
      ++lineno;
      std::string line;
      std::getline(file, line);
      std::string::size_type i = 0;
      // Skip over leading whitespace.
      while (i < line.length() && std::isspace(line[i]))
         ++i;
      if ( i == line.length() || line[i] == '#' )
         continue;
      if ( i == line.length() - 1 || !std::isspace(line[i + 1]) ) {
         throw ioutil::Exception("Invalid line");
      }
      char const type = line[i++];
      while (i < line.length() && std::isspace(line[i]))
         ++i;
      
      if ( type == 'I' ) {
         std::stringstream s(line.substr(i), std::ios_base::in);
         Investment::ID_TYPE id;
         double ret;
         std::string name;
         s >> id >> ret;
         if ( !s )
            throw ioutil::Exception("Invalid investment specification");
         std::getline(s, name);
         while (name.size() > 0 && std::isspace(name[0]))
            name = name.substr(1);
         if ( !name.size() )
            throw ioutil::Exception("Invalid investment specification");
         Investment const inv(id, name, ret);
         if ( is.find(inv.getId()) != is.end() )
            throw ioutil::Exception("Duplicate investment");
         is.insert(std::pair<Investment::ID_TYPE, Investment>(inv.getId(), inv));
      }
      else if ( type == 'C' ) {
         std::stringstream s(line.substr(i), std::ios_base::in);
         Investment::ID_TYPE id1, id2;
         double cov;
         s >> id1 >> id2 >> cov;
         if ( !s )
            throw ioutil::Exception("Invalid covariance specification");
         triples.push_back(ioutil::Triple(id1, id2, cov));
      }
      else
         throw ioutil::Exception("Invalid line");
   }

   for (std::map<Investment::ID_TYPE, Investment>::const_iterator it = is.begin();
        it != is.end(); ++it)
      *investments++ = it->second;
   for (typename std::vector<ioutil::Triple>::const_iterator it = triples.begin();
        it != triples.end(); ++it)
   {
      if ( is.find(it->id1) != is.end() &&
           is.find(it->id2) != is.end() )
         covariance.setCovariance(it->id1, it->id2, it->covariance);
   }
}

} // portfolio
} // cpx

#endif // !CPXPORTFOLIOINVESTMENT
