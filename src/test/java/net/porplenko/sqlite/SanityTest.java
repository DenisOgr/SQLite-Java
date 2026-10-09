package net.porplenko.sqlite;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SanityTest {

    @Test
    void buildIsGreen() {
        assertThat(1 + 1).isEqualTo(2);
    }
}
