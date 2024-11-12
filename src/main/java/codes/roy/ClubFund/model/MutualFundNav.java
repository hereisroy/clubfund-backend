package codes.roy.ClubFund.model;

import java.time.LocalDate;

import codes.roy.ClubFund.bean.MutualFundNavId;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;

@Entity
@IdClass(MutualFundNavId.class)
public class MutualFundNav {

	@ManyToOne @Id
	private MutualFund fund;
	@Id
	private LocalDate date;
	private double nav;
	private boolean isInvestmentDate;

	public MutualFundNav() {
		super();
	}
	public MutualFundNav(MutualFund fund, LocalDate date, double nav, boolean isInvestmentDate) {
		super();
		this.fund = fund;
		this.date = date;
		this.nav = nav;
		this.isInvestmentDate = isInvestmentDate;
	}
	public MutualFund getFund() {
		return fund;
	}
	public void setFund(MutualFund fund) {
		this.fund = fund;
	}
	public LocalDate getDate() {
		return date;
	}
	public void setDate(LocalDate date) {
		this.date = date;
	}
	public double getNav() {
		return nav;
	}
	public void setNav(double nav) {
		this.nav = nav;
	}
	public boolean isInvestmentDate() {
		return isInvestmentDate;
	}
	public void setInvestmentDate(boolean isInvestmentDate) {
		this.isInvestmentDate = isInvestmentDate;
	}

}
