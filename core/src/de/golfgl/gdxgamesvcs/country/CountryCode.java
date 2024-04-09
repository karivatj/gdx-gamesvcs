package de.golfgl.gdxgamesvcs.country;

public class CountryCode implements ICountryCode {

      private String countryCode;

      public CountryCode(String countryCode) {
          this.countryCode = countryCode;
      }

      @Override
      public String getCountryCode() {
          return countryCode;
      }

      @Override
      public void setCountryCode(String countryCode) {
          this.countryCode = countryCode;
      }
}
