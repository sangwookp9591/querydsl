package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.support.PageableExecutionUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

import javax.persistence.EntityManager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import study.querydsl.entity.Member;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

// 꼭 impl이라는 이름으로 만들어야한다.
public class MemberRepositoryImpl implements MemberRepositoryCustom{

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
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

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory.select(new QMemberTeamDto(
                member.id.as("memberId")
                , member.username
                , member.age
                , team.id.as("teamId")
                , team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername())
                        , teamNameEq(condition.getTeamName())
                        , ageGoe(condition.getAgeGoe())
                        , ageLoe(condition.getAgeLoe())
                ).offset(pageable.getOffset()) //몇번부터 시작할거야
                .limit(pageable.getPageSize())
                .fetchResults();//content용 쿼리 count용쿼리 2번날림
        List<MemberTeamDto> content = results.getResults();
        long total = results.getTotal();

        return new PageImpl<>(content,pageable,total);

    }

    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable) {

        List<MemberTeamDto> content = queryFactory.select(new QMemberTeamDto(
                member.id.as("memberId")
                , member.username
                , member.age
                , team.id.as("teamId")
                , team.name.as("teamName")))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername())
                        , teamNameEq(condition.getTeamName())
                        , ageGoe(condition.getAgeGoe())
                        , ageLoe(condition.getAgeLoe())
                ).offset(pageable.getOffset()) //몇번부터 시작할거야
                .limit(pageable.getPageSize())
                .fetch();//content용 쿼리 count용쿼리 2번날림

        JPAQuery<Member> countQuery = queryFactory.select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(condition.getUsername())
                        , teamNameEq(condition.getTeamName())
                        , ageGoe(condition.getAgeGoe())
                        , ageLoe(condition.getAgeLoe())
                );

        return PageableExecutionUtils.getPage(content,pageable,()-> countQuery.fetchCount());
        //이렇게 하면 함수이기때문에 구문이 실행이안되고 getPage에서 conten와 pageable을 보고 페이지의 시작이면서  content사이즈가 page사이즈보다 작거나 마지막 사이즈면 countQuery를 getPage안에서 호출 안한다.
        //return PagableExceutionUtils.get

        //return new PageImpl<>(content,pageable,total);
    }
}
