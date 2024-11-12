package codes.roy.ClubFund.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class MutualFund {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private int fundId;
	private String fundName;
	@Column(unique = true)
	private String apiURL;
	private double netInvestment;
	private double currentValue;
	private double netReturns;
	private double xirr;



	public MutualFund(String fundName, String apiURL) {
		super();
		this.fundName = fundName;
		this.apiURL = apiURL;
	}
	public MutualFund() {
		super();
	}
	public int getFundId() {
		return fundId;
	}
	public void setFundId(int fundId) {
		this.fundId = fundId;
	}
	public String getFundName() {
		return fundName;
	}
	public void setFundName(String fundName) {
		this.fundName = fundName;
	}
	public String getApiURL() {
		return apiURL;
	}
	public void setApiURL(String apiURL) {
		this.apiURL = apiURL;
	}
	public double getNetInvestment() {
		return netInvestment;
	}
	public void setNetInvestment(double netInvestment) {
		this.netInvestment = netInvestment;
	}
	public double getCurrentValue() {
		return currentValue;
	}
	public void setCurrentValue(double currentValue) {
		this.currentValue = currentValue;
	}
	public double getNetReturns() {
		return netReturns;
	}
	public void setNetReturns(double netReturns) {
		this.netReturns = netReturns;
	}
	public double getXirr() {
		return xirr;
	}
	public void setXirr(double xirr) {
		this.xirr = xirr;
	}



}
