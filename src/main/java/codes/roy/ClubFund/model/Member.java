package codes.roy.ClubFund.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Member {

	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private int memberId;
	private String name;
	@Column(unique = true)
	private String email;
	private String pwdHash;
	private double rpi; // Required Monthly Investment
	private double cvp; // Contribution Via Principle
	private double cvi; // Contribution Via Interest
	private double nc; // Net Contribution
	private boolean isAdmin=false;

	public Member(String name, String email, String pwdHash){
		this.name = name;
		this.email = email;
		this.pwdHash = pwdHash;
		this.rpi = 0;
		this.cvp = 0;
		this.cvi = 0;
		this.nc = 0;
	}

	public Member(){
		super();
	}

	public int getMemberId() {
		return memberId;
	}
	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getPwdHash() {
		return pwdHash;
	}
	public void setPwdHash(String pwdHash) {
		this.pwdHash = pwdHash;
	}
	public double getRpi() {
		return rpi;
	}
	public void setRpi(double rmi) {
		this.rpi = rmi;
	}
	public double getCvp() {
		return cvp;
	}
	public void setCvp(double cvp) {
		this.cvp = cvp;
	}
	public double getCvi() {
		return cvi;
	}
	public void setCvi(double cvi) {
		this.cvi = cvi;
	}
	public double getNc() {
		return nc;
	}
	public void setNc(double nc) {
		this.nc = nc;
	}
	public boolean isAdmin() {
		return isAdmin;
	}
	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
}
