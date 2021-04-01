package study.querydsl.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;

import javax.persistence.EntityManager;
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
}
