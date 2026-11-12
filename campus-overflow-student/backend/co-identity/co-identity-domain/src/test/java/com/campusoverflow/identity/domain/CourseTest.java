package com.campusoverflow.identity.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import com.campusoverflow.shared.domain.ForbiddenException;
import com.campusoverflow.shared.security.Actor;
import com.campusoverflow.shared.security.Role;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CourseTest {

    private static final Instant NOW = Instant.parse("2026-09-01T08:00:00Z");

    @Test
    void teacherCreatesCourseAndBecomesItsTeacher() {
        Course c = Course.create(new Actor(5, "王老师", Role.TEACHER), "ds-101", "数据结构", "2026-秋", NOW);
        assertThat(c.code()).isEqualTo("DS-101");
        assertThat(c.teacherId()).isEqualTo(5);
    }

    @Test
    void studentCannotCreateCourse() {
        assertThatThrownBy(() -> Course.create(new Actor(2, "alice", Role.STUDENT), "DS", "数据结构", "2026-秋", NOW))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void courseCodeMustBeTwoToTwentyLettersDigitsOrHyphens() {
        Actor teacher = new Actor(5, "王老师", Role.TEACHER);
        assertThatThrownBy(() -> Course.create(teacher, "D", "数据结构", "2026-秋", NOW))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("课程代码");
        assertThatThrownBy(() -> Course.create(teacher, "数据结构101", "数据结构", "2026-秋", NOW))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("课程代码");
        assertThat(Course.create(teacher, "ds-101", "数据结构", "2026-秋", NOW).code()).isEqualTo("DS-101");
    }

    @Test
    void courseNameAndTermMustBeWithinBounds() {
        Actor teacher = new Actor(5, "王老师", Role.TEACHER);
        assertThatThrownBy(() -> Course.create(teacher, "DS-101", "数", "2026-秋", NOW))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("课程名称");
        assertThatThrownBy(() -> Course.create(teacher, "DS-101", "字".repeat(51), "2026-秋", NOW))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("课程名称");
        assertThatThrownBy(() -> Course.create(teacher, "DS-101", "数据结构", "秋", NOW))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("学期");
        assertThat(Course.create(teacher, "DS-101", "数据结构", "2026-秋", NOW).term()).isEqualTo("2026-秋");
    }

    @Test
    void courseTeacherRoleCannotBeDowngraded() {
        CourseMember m = CourseMember.teacher(1, 5, NOW);
        assertThatThrownBy(() -> m.changeRole(CourseRole.STUDENT)).isInstanceOf(BusinessRuleException.class);
        CourseMember s = CourseMember.student(1, 6, NOW);
        s.changeRole(CourseRole.TA);
        assertThat(s.role().canManage()).isTrue();
    }
}
