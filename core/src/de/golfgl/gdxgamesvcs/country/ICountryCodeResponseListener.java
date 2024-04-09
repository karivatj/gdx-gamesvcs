package de.golfgl.gdxgamesvcs.country;

public interface ICountryCodeResponseListener {
    void countryCodeReceived(String countryCode);
    void countryCodeRequestFailed(String errorMsg);
}
