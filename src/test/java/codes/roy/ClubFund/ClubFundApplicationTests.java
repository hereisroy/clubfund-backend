package codes.roy.ClubFund;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import codes.roy.ClubFund.bean.MemberTokenDetails;
import codes.roy.ClubFund.repository.AllDataRepository;
import codes.roy.ClubFund.service.AdminActionService;
import codes.roy.ClubFund.service.MemberActionService;
import codes.roy.ClubFund.util.AccessTokenUtil;
import codes.roy.ClubFund.util.ClubFundUtil;

@SpringBootTest
@Transactional
@Rollback(false)
class ClubFundApplicationTests {

	@Autowired AllDataRepository repo;
	@Autowired ClubFundUtil util;
	@Autowired AdminActionService adminService;
	@Autowired MemberActionService memberService;
	@Autowired BCryptPasswordEncoder encoder;
	@Autowired AccessTokenUtil tokenUtil;
	
//	@Test
	public void miscTest() {
		try {
			assertTrue(true);

		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
