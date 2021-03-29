package study.querydsl.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Getter @Setter
@ToString(of = {"id","username","age"}) //team 이런것은 들어가면안된다 왓다리갓다리한다.
public class Member {

    @Id @GeneratedValue
    @Column(name ="member_id")
    private Long id;
    private String username;
    private int age;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    public Member(String username){
        this(username,0);
    }

    public Member(String username,int age){

        this(username,age,null);
    }
    public Member(String username, int age,Team team){
        this.username = username;
        this.age =age;
        if(team != null){
            chageTeam(team);
        }
    }

    private void chageTeam(Team team) {
        this.team =team;
        team.getMembers().add(this);
    }



}
