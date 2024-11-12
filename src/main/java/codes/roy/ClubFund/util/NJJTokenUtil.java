package codes.roy.ClubFund.util;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;

import codes.roy.ClubFund.bean.MemberTokenDetails;

// AccessTokenUtil impl using Nimbus Jose JWT

@Component
public class NJJTokenUtil implements AccessTokenUtil{

	@Override
	public MemberTokenDetails validateToken(String jwtStr) {
		MemberTokenDetails tokenDetails = null;
		try {
			JWSObject jwsObject = JWSObject.parse(jwtStr);
			JWSVerifier verifier = new MACVerifier(ClubFundUtil.getSecretBytes());
			if(jwsObject.verify(verifier)) {
				Map<String, Object> payload = jwsObject.getPayload().toJSONObject();
				if(!payload.get("iss").toString().equals(ClubFundUtil.getIssuer())) {
					return null;
				}
				Date expDate = new Date((long)payload.get("exp")*1000);
				Date now = new Date();
				if(expDate.before(now)) {
					return null;
				}
				tokenDetails = new MemberTokenDetails(Integer.parseInt(payload.get("member_id").toString()), payload.get("sub").toString(), payload.get("role").toString().equals("ADMIN")? true : false);
			}
		} catch(ParseException | JOSEException e) {
			e.printStackTrace();
		}

		return tokenDetails;
	}

	@Override
	public String createToken(MemberTokenDetails memberDetails) {
		JWSObject jws = null;
		try {
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.DAY_OF_MONTH, 7);
			Date expDate = calendar.getTime();
			JWSSigner signer = new MACSigner(ClubFundUtil.getSecretBytes());
			JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
			JWTClaimsSet claims = new JWTClaimsSet.Builder()
			.issuer(ClubFundUtil.getIssuer())
			.subject(memberDetails.getEmail())
			.claim("member_id", memberDetails.getMemberId())
			.claim("role", memberDetails.isAdmin()? "ADMIN" : "MEMBER")
			.issueTime(new Date())
			.expirationTime(expDate)
			.build();
			jws = new JWSObject(header, claims.toPayload());
			jws.sign(signer);

		} catch (JOSEException e) {
			e.printStackTrace();
		}
		return jws==null?"":jws.serialize();
	}

}
