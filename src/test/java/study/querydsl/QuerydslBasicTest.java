package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before(){
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1",10,teamA);
        Member member2 = new Member("member2",20,teamA);

        Member member3 = new Member("member3",30,teamB);
        Member member4 = new Member("member4",40,teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        //초기화
        em.flush();
        em.clear();

        //확인
        List<Member> members = em.createQuery("select m from Member m",Member.class)
                .getResultList();

        for (Member member : members) {
            System.out.println("member = "+member);
            System.out.println("member.getTeam() = " + member.getTeam());

        }
    }

    @Test
    public void startJPQL() {
        //member1을 찾아라.
        String qlString =
                "select m from Member m " +
                        "where m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

         assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public  void startQuerydsl(){
       // queryFactory =  new JPAQueryFactory(em); //동시성 문제없이 설계되어있음
      //  QMember m = new QMember("m");//변수명에 별칭 이름을 줘야한다. ->어떤 QMember지 구분하는 이름
        //나중에는 안쓴다 변수명에 별칭쓰는것을


        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) //그냥 문자면 SQL INjection 공격을 받을 수 도 있지만 querydsl은 자동으로 prepareStateMent의 파라미터 바인딩을 방식을 사용 .
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age
                        .eq(10))).fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }


    @Test
    public void resultFetch(){
        List<Member> fetch = queryFactory.selectFrom(member).fetch();
        Member fetchOne = queryFactory.selectFrom(QMember.member).fetchOne();

        Member fetchFirst = queryFactory.selectFrom(QMember.member).fetchFirst();

        QueryResults<Member> results = queryFactory.selectFrom(member).fetchResults();

        results.getTotal();
        List<Member> content = results.getResults();

        long total = queryFactory
                .selectFrom(member)
                .fetchCount();

    }
}
