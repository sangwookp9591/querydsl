package study.querydsl.entity;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(of = {"id","name"})
public class Team {

    @Id @GeneratedValue
    private long id;
    private String name;

    @OneToMany(mappedBy = "team")//연관관계주인 아님
    private List<Member> members = new ArrayList<>();

    public Team(String name){
        this.name = name;
    }


}
