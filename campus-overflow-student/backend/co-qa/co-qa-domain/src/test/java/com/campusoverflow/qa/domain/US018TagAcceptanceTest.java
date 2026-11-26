package com.campusoverflow.qa.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusoverflow.shared.domain.BusinessRuleException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** US-018 标签验收：数量 1-5、长度 30、字符集、去重与归一。 */
class US018TagAcceptanceTest {

    @Test
    @DisplayName("单个标签可以提交")
    void singleTagAccepted() {
        Tags tags = Tags.of(List.of("数据结构"));

        assertThat(tags.names()).containsExactly("数据结构");
    }

    @Test
    @DisplayName("5 个标签可以提交")
    void fiveTagsAccepted() {
        Tags tags = Tags.of(List.of("java", "spring", "数据库", "索引", "性能优化"));

        assertThat(tags.values()).hasSize(5);
    }

    @Test
    @DisplayName("0 个标签被拒绝")
    void zeroTagsRejected() {
        assertThatThrownBy(() -> Tags.of(List.of()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("1–5");
    }

    @Test
    @DisplayName("6 个标签被拒绝")
    void sixTagsRejected() {
        assertThatThrownBy(() -> Tags.of(List.of("a", "b", "c", "d", "e", "f")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("1–5");
    }

    @Test
    @DisplayName("null 标签集合被拒绝")
    void nullTagsRejected() {
        assertThatThrownBy(() -> Tags.of(null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("重复标签自动去重")
    void duplicateTagsCollapsed() {
        Tags tags = Tags.of(List.of("java", "JAVA", "java"));

        assertThat(tags.values()).hasSize(1);
        assertThat(tags.names()).containsExactly("java");
    }

    @Test
    @DisplayName("标签统一转小写并去掉前缀井号")
    void tagNormalized() {
        assertThat(new TagName("  #DataStructure ").value()).isEqualTo("datastructure");
    }

    @Test
    @DisplayName("允许 c++、c#、.net 类符号与中文")
    void symbolTagsAccepted() {
        assertThat(new TagName("c++").value()).isEqualTo("c++");
        assertThat(new TagName("c#").value()).isEqualTo("c#");
        assertThat(new TagName("2026秋").value()).isEqualTo("2026秋");
    }

    @Test
    @DisplayName("标签长度为 30 时通过，31 时被拒绝")
    void lengthBoundary() {
        assertThat(new TagName("a".repeat(30)).value()).hasSize(30);

        assertThatThrownBy(() -> new TagName("a".repeat(31)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("标签格式");
    }

    @Test
    @DisplayName("空标签被拒绝")
    void blankTagRejected() {
        assertThatThrownBy(() -> new TagName("   "))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new TagName(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("不能为空");
    }

    @Test
    @DisplayName("非法字符被拒绝")
    void illegalCharacterRejected() {
        assertThatThrownBy(() -> new TagName("数据 结构"))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> new TagName("a/b"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("以符号开头的标签被拒绝")
    void leadingSymbolRejected() {
        assertThatThrownBy(() -> new TagName("+java"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
