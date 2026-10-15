package net.porplenko.sqlite.page;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextEncodingTest {
    @Test
    void fromCodeSuccessTest() {
        assertThat(TextEncoding.fromCode(1)).isEqualTo(TextEncoding.UTF_8);
        assertThat(TextEncoding.fromCode(2)).isEqualTo(TextEncoding.UTF_16_LE);
        assertThat(TextEncoding.fromCode(3)).isEqualTo(TextEncoding.UTF_16_BE);
    }

    @Test
    void fromCodeFailureTest() {
        assertThatThrownBy(() -> TextEncoding.fromCode(100))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid code:");
    }
}
