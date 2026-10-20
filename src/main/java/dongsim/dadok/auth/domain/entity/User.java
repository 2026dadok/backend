package dongsim.dadok.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import javax.management.relation.Role;
import java.time.LocalDate;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    private Integer defaultTimerTime; // 기본 타이머 시간 (단위: 초 또는 분)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }
}