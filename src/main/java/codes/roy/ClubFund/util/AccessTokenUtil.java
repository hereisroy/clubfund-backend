package codes.roy.ClubFund.util;

import codes.roy.ClubFund.bean.MemberTokenDetails;

public interface AccessTokenUtil {

	public MemberTokenDetails validateToken(String jwtStr); // returns null if validation fails

	public String createToken(MemberTokenDetails memberDetails);
}
