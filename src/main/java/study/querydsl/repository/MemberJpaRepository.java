package study.querydsl.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;

import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import java.util.Optional;

import static org.springframework.util.StringUtils.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.*;

@Repository //데이터를 접근하는 계층
public class MemberJpaRepository {

    private final EntityManager em;

    private final JPAQueryFactory queryFactory;
    //동시성문제가 생기지 않나? ->
    // @Bean에서 등록한것은 싱글톤인데 같은 객체를 모든 멀티쓰레드에서 다쓰는데 문제가 없을까?
    //없다 왜냐하면 어차피 JPAQueryFactory에 대한 동시성 문제는 EnitityManager에 다의존한다.
    // EntityManager가 스프링에 엮어쓰면 동시성 문제랑 관계없이 트랜잭션딴위로 따로따로 동작하게된다.
    //EntityMmanger가 진짜 영속성컨텍스트 em이 아니라 프록시를 주입해준다.
    //얘는 트랜잭션단위로 다 다른데 바인딩 되도록 라우팅만 해주는 역할을 한다.
    //

    //Spring bean 생성하는 방법 ,얘는 인젝션을 두개해줘야해서 귀찮고
//    public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory) {
//        this.em = em;
//        this.queryFactory = queryFactory;
//    }

    //테스트 코드짤때 편함 주입받는게 하나이기때문
    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);

    }

    public void save(Member member){
        em.persist(member);
    }

    public Optional<Member> findById(Long id){
        Member findMember = em.find(Member.class,id);
        return Optional.ofNullable(findMember);
    } //null일 수도 있으니깐 Optional로 반환

    public List<Member> findAll(){
        List<Member> members = em.createQuery("select m from Member m", Member.class).getResultList();
        return members;

    }
    public List<Member> findAll_Querydsl(){
        return queryFactory.selectFrom(member).fetch();
    }

    public List<Member> findByUsername(String username){
        return em.createQuery("select m.username from Member m where m.username =:username",Member.class)
                .setParameter("username",username).getResultList();
    }
    public List<Member> findByUsername_Querydsl(String username){
        return queryFactory.selectFrom(member).where(member.username.eq(username)).fetch();

    }

    public List<MemberTeamDto> searchByBuilder(MemberSearchCondition condition){
        BooleanBuilder builder = new BooleanBuilder();
        //아래의 조건들이 돌아가게 하기 위해서
        if (hasText(condition.getUsername())) {
            builder.and(member.username.eq(condition.getUsername()));
        }

        if (hasText(condition.getTeamName())) {
            builder.and(team.name.eq(condition.getTeamName()));
        }

        if (condition.getAgeGoe() != null) {
            builder.and(member.age.goe(condition.getAgeGoe()));
        }

        if (condition.getAgeLoe() != null) {
            builder.and(member.age.loe(condition.getAgeLoe()));
        }

        return queryFactory
                .select(new QMemberTeamDto(
                        member.id.as("memberId")
                        ,member.username
                        ,member.age
                        ,team.id.as("teamId")
                        ,team.name.as("teamName")))
                .from(member)
                .join(member.team, team)
                .fetch();
    }

    public List<MemberTeamDto> search(MemberSearchCondition condition){
        //builder를 쓰면 조건문이 들어가기때문에 눈으로 봐야한다.
        //하지만  where절에 하면 쿼리처럼 볼수있다.
        //재사용이 가능하다.
        return queryFactory.select(new QMemberTeamDto(
                member.id.as("memberId")
                ,member.username
                ,member.age
                ,team.id.as("teamId")
                ,team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team,team)
                .where(
                        usernameEq(condition.getUsername())
                        ,teamNameEq(condition.getTeamName())
                        ,ageGoe(condition.getAgeGoe())
                        ,ageLoe(condition.getAgeLoe())
                ).fetch();
    }

    //기본적으로 Predicate가 반환타입으로 나와있는데 이것보단 BooleanExpression이 낫다
    //BooleanExpression은 조합 할 수 있다.
    private BooleanExpression usernameEq(String username) {
        return hasText(username) ?  member.username.eq(username):null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName) ?  team.name.eq(teamName) :null;
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe!=null ? member.age.goe(ageGoe):null;
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe!=null ? member.age.goe(ageLoe):null;

    }


    private BooleanExpression ageBetween(int ageLoe, int ageGoe){
        return ageGoe(ageGoe).and(ageGoe(ageGoe));

    }

}
