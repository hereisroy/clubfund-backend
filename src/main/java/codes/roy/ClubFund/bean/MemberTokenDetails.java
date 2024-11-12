package codes.roy.ClubFund.bean;

public class MemberTokenDetails {
	private int memberId;
	private String email;
	private boolean isAdmin;

	public MemberTokenDetails(int memberId, String email, boolean isAdmin) {
		super();
		this.memberId = memberId;
		this.email = email;
		this.isAdmin = isAdmin;
	}
	public MemberTokenDetails() {
		super();
		// TODO Auto-generated constructor stub
	}
	public int getMemberId() {
		return memberId;
	}
	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public boolean isAdmin() {
		return isAdmin;
	}
	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
}
