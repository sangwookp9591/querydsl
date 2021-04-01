package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Profile("local")
@Component //Spring bean에 자동으로 등록되게 만듦
@RequiredArgsConstructor
public class InitMember {

    //appication을 실행시키면 application.yml에 profile active를 local로 설정해노았기 떄문에
    //스프링부트로 메인을 실행하면 local로 실행되면서 @PostConstuct가 실행된다.

    private final InitMemberService initMemberService;

    @PostConstruct
    public void init(){
        initMemberService.init();
    }

    @Component
    static class InitMemberService{

        @PersistenceContext
        EntityManager em;

        @Transactional
        public void init(){
            Team teamA = new Team("teamA");
            Team teamB = new Team("teamB");
            em.persist(teamA);
            em.persist(teamB);

            for(int i = 0; i<100;i++){
                Team selectedTeam = i%2 ==0 ?teamA : teamB;
                em.persist(new Member("member"+i,i,selectedTeam));
            }
        }
    }
}
