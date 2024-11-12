package codes.roy.ClubFund.model;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

@Entity
public class Investment {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private int investmentId;
	@DateTimeFormat(pattern = "yyyy-MM-dd")
	private LocalDate investmentDate;
	@ManyToOne
	private MutualFund fund;
	private double ammountInvested;
	private double currentValue;
	private double returns;
	@OneToMany(mappedBy = "investment", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
	private List<Contribution> contributionList;
	private boolean isNavValuesPresent;
	public int getInvestmentId() {
		return investmentId;
	}
	public void setInvestmentId(int investmentId) {
		this.investmentId = investmentId;
	}
	public LocalDate getInvestmentDate() {
		return investmentDate;
	}
	public void setInvestmentDate(LocalDate investmentDate) {
		this.investmentDate = investmentDate;
	}
	public MutualFund getFund() {
		return fund;
	}
	public void setFund(MutualFund fund) {
		this.fund = fund;
	}
	public double getAmmountInvested() {
		return ammountInvested;
	}
	public void setAmmountInvested(double ammountInvested) {
		this.ammountInvested = ammountInvested;
	}
	public double getCurrentValue() {
		return currentValue;
	}
	public void setCurrentValue(double currentValue) {
		this.currentValue = currentValue;
	}
	public double getReturns() {
		return returns;
	}
	public void setReturns(double returns) {
		this.returns = returns;
	}
	public List<Contribution> getContributionList() {
		return contributionList;
	}
	public void setContributionList(List<Contribution> contributionList) {
		this.contributionList = contributionList;
	}
	public boolean isNavValuesPresent() {
		return isNavValuesPresent;
	}
	public void setNavValuesPresent(boolean isNavValuesPresent) {
		this.isNavValuesPresent = isNavValuesPresent;
	}
}
