package codes.roy.ClubFund.bean;

import java.io.Serializable;
import java.time.LocalDate;

import codes.roy.ClubFund.model.MutualFund;

public class MutualFundNavId implements Serializable {

	private static final long serialVersionUID = 1L;
	private MutualFund fund;
	private LocalDate date;
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


}
