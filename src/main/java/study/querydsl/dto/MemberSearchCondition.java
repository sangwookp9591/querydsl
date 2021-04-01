package study.querydsl.dto;

import lombok.Data;

@Data
public class MemberSearchCondition {
    //회원명, 팀명, 나이(ageGoe,ageLoe)

    private String username;
    private String teamName;
    private Integer ageGoe;//Integer를 쓴이유는 값이 null일수 있어서.
    private Integer ageLoe;



}
