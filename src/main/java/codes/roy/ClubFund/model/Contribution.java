package codes.roy.ClubFund.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Contribution {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private int contributionId;
	@ManyToOne
	private Member contributor;
	private int ammount;
	@ManyToOne
	@JsonIgnore
	private Investment investment;
	public int getContributionId() {
		return contributionId;
	}
	public void setContributionId(int contributionId) {
		this.contributionId = contributionId;
	}
	public Member getContributor() {
		return contributor;
	}
	public void setContributor(Member contributor) {
		this.contributor = contributor;
	}
	public int getAmmount() {
		return ammount;
	}
	public void setAmmount(int ammount) {
		this.ammount = ammount;
	}
	public Investment getInvestment() {
		return investment;
	}
	public void setInvestment(Investment investment) {
		this.investment = investment;
	}

}
