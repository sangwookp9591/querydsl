package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {
    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @PersistenceUnit
    EntityManagerFactory emf;


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

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순( desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     *
     *
     * */

    @Test
    public void sort(){
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();


        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults(); //카운트 쿼리 추가

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }


    /**
     * JPQL
     * select
     * COUNT(m), //회원수
     * SUM(m.age), //나이 합
     * AVG(m.age), //평균 나이
     * MAX(m.age), //최대 나이
     * MIN(m.age) //최소 나이
     * from Member m
     */
    @Test
    public void aggregation() throws Exception {
        List<Tuple> result = queryFactory
                .select(member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);
        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /**
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() throws Exception {
        QMember member = QMember.member;
        QTeam team = QTeam.team;
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }


    /**
     * 세타 조인(연관관계가 없는 필드로 조인)
     * 회원의 이름이 팀 이름과 같은 회원 조회
     */
    @Test
    public void theta_join() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }


    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and
     t.name='teamA'
     */
    @Test
    public void join_on_filtering() throws Exception {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 2. 연관관계 없는 엔티티 외부 조인
     * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     */
    @Test
    public void join_on_no_relation() throws Exception {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name))
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("t=" + tuple);
        }
    }

//패치조인이 없을때
    @Test
    public void fetchJoinNo() throws Exception {
        em.flush(); //패치조인을 할떄는 영속성컨텍스트에 있는 애들을 안지워주면 결괄르 보기 힘들다.
        em.clear();
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        boolean loaded =
                emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam()); //결과가 이미 초기화(로딩)되 엔티티인디 아니면 초기화(로딩)가 안된 엔티티인지 알려주는 기능
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse() throws Exception {
        em.flush();
        em.clear();
        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();
        boolean loaded =
                emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery() throws Exception {
        //서브쿼리이기때문에 밖의 member랑 겹치면 안되기떄문에 aias를 새로 생성을 해야한다
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                )).fetch();

        assertThat(result).extracting("age").containsExactly(40);
    }

    /**
     * 나이가 평균 나이 이상인 회원
     */
    @Test
    public void subQueryGoe() throws Exception {
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .select(member)
                .from(member)
                .where(member.age.goe(
                        select(memberSub.age.avg()).from(memberSub))
                ).fetch();
        assertThat(result).extracting("age").containsExactly(30, 40);

    }

    /**
     * 서브쿼리 여러 건 처리, in 사용
     */
    @Test
    public void subQueryIn() throws Exception {
        //where 절 서브쿼리
        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .select(member)
                .from(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10)))
                ).fetch();
        assertThat(result).extracting("age").containsExactly(20,30, 40);



//        QMember memberSub = new QMember("memberSub");
//        List<Member> result = queryFactory
//                .selectFrom(member)
//                .where(member.age.in(
//                        JPAExpressions
//                                .select(memberSub.age)
//                                .from(memberSub)
//                                .where(memberSub.age.gt(10))
//                ))
//                .fetch();
//        assertThat(result).extracting("age")
//                .containsExactly(20, 30, 40);
//
//        List<Tuple> fetch = queryFactory
//                .select(member.username,
//                        JPAExpressions
//                                .select(memberSub.age.avg())
//                                .from(memberSub)
//                ).from(member)
//                .fetch();
//        for (Tuple tuple : fetch) {
//            System.out.println("username = " + tuple.get(member.username));
//            System.out.println("age = " +
//                    tuple.get(JPAExpressions.select(memberSub.age.avg())
//                            .from(memberSub)));
//        }
//
//        import static com.querydsl.jpa.JPAExpressions.select;
//        List<Member> result = queryFactory
//                .selectFrom(member)
//                .where(member.age.eq(
//                        select(memberSub.age.max())
//                                .from(memberSub)
//                ))
//                .fetch();
    }

    @Test
    public void selectSubquery(){
        //select절 서브쿼리
        QMember memberSub = new QMember("memberSub");
        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }
    //단순한 case
    @Test
    public void basicCase(){
        List<String> result = queryFactory.select(
                member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);

        }
    }
    //복잡한 case
    @Test
    public void complexCase(){
        List<String> result = queryFactory
                .select(new CaseBuilder().when(member.age.between(0, 20)).then("0~20")
                        .when(member.age.between(21, 30)).then("21~30")
                        .otherwise("기타")
                ).from(member).fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }

    }

    //상수가 필요할 때
    @Test
    public void constant(){
        List<Tuple> result = queryFactory.select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    //문자열 합치기
    @Test
    public void concat(){
        //queryFactory.select(member.username.concat("_").concat(member.age))

        //이건 안된다. 왜냐하면 타입이다르다 concat은 문자만 되기 때문이다.

        //원하는 결과
        //{username}_{age}
        List<String> result = queryFactory.select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();


        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void simpleProjection(){
        List<String> result = queryFactory.select(member.username).from(member).fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection() {
        List<Tuple> fetch = queryFactory.select(member.username, member.age).from(member).fetch();


        for (Tuple tuple : fetch) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("age = " + age);
            System.out.println("username = " + username);
        }
    }

    @Test
    public void findDtoByJPQL(){
        //이렇게 하면 타입이 안맞아서 안된다.
        // .em.createQuery("select m from Member  m", MemberDto.class);

        //jpql에서 제공하는 new operation 사용
        List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username,m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : resultList) {

            System.out.println("memberDto = " + memberDto);

        }
    }

    @Test
    public void findBySetter(){
        queryFactory
                .select(Projections.bean(MemberDto.class,member.username,member.age))
                .from(member)
                .fetch(); //bean은 setter로 데이터를 injection 해줌
    }

    @Test //getter , setter 필요없다. 바로 필드에다가 값을 넣어버림
    public void findByField(){
        queryFactory
                .select(Projections.fields(MemberDto.class,member.username,member.age))
                .from(member)
                .fetch();
    }

    @Test
    public void findDtoByConstructor(){
        List<MemberDto> result = queryFactory.select(Projections.constructor(MemberDto.class, member.username, member.age
        )).from(member).fetch();
        for(MemberDto memberDto : result){
            System.out.println("memberDto = " + memberDto);
        }
    }


    @Test
    public void findUserDto(){
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory.select(Projections.fields(UserDto.class
                , member.username.as("name")
                , ExpressionUtils.as(select(memberSub.age.max()).from(memberSub), "age") //다 최대인 40살로 찍고싶은 경우
        ))
                .from(member).fetch();

    }


    @Test
    public void findDtoByQueryProjection(){
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }


    @Test
    public void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam,ageParam);

        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder =new BooleanBuilder();
        if(usernameCond != null){
            builder.and(member.username.eq(usernameCond));
        }
        if(ageCond != null){
            builder.and(member.age.eq(ageCond));
        }

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();


        return result;
    }


    @Test
    public void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam,ageParam);

        Assertions.assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {

        return queryFactory
                .selectFrom(member)
                //.where(usernameEq(usernameCond),ageEq(ageCond))
                .where(allEq(usernameCond,ageCond))
                .fetch();


    }

    //메서드로 빠졋기때문에 얻는 이점
    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null ;//null을 반환하면 ? where(null. 이렇게되는데 querydsl은 where에 null이 들어오면 무시한다.
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ?  member.age.eq(ageCond) : null;
    }

    private BooleanExpression allEq(String usernameCond,Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
    public void bulkUpdate(){

        //member1  = 10 ->비회원
        //member2 = 20 -> 비회원
        //member3 = 30 -> 유지
        //member4 = 40 -> 유지
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        em.flush();;
        em.clear();

        //벌크연산이 나가면 이미 db랑 연속성 컨텍스트랑 안맞기때문에 db를 초기화해버리는게 낫다
        //member1  = 10 ->db비회원
        //member2 = 20 -> db비회원
        //member3 = 30 -> db member3
        //member4 = 40 -> db member4

        List<Member> reuslt = queryFactory.selectFrom(member).fetch();

        for (Member member1 : reuslt) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    public void bulkAdd(){
        //나이 더하기 1
        long count = queryFactory.update(member)
                .set(member.age
                        , member.age.add(1))
                .execute();

        //s나이 곱하기2
        long count2 = queryFactory.update(member)
                .set(member.age
                        , member.age.multiply(2))
                .execute();
    }

    @Test
    public void bulkDelete(){
        queryFactory.delete(member)
                .where(member.age.gt(18)).execute();
    }

    @Test
    public void sqlFunction(){
        List<String> result = queryFactory.select(Expressions.stringTemplate(
                "function('replace',{0},{1},{2})",
                member.username, "member", "M"))
                .from(member)
                .fetch();// 조회를 할건데 Member라는 단어를 M으로 바꿔서 출력
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }


    @Test
    public void sqlFunction2(){
        List<String> result = queryFactory.select(member.username)
                .from(member)
                //.where(member.username.eq(Expressions.stringTemplate("function('lower',{0})",member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }


    }
}
