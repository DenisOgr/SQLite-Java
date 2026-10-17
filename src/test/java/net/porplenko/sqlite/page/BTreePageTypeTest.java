package net.porplenko.sqlite.page;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BTreePageTypeTest {
    @Test
    void fromCodeValidCode() {
        assertThat(BTreePageType.fromCode(2)).isEqualTo(BTreePageType.INTERIOR_INDEX);
        assertThat(BTreePageType.fromCode(5)).isEqualTo(BTreePageType.INTERIOR_TABLE);
        assertThat(BTreePageType.fromCode(10)).isEqualTo(BTreePageType.LEAF_INDEX);
        assertThat(BTreePageType.fromCode(13)).isEqualTo(BTreePageType.LEAF_TABLE);
    }

    @Test
    void fromCodeInValidCode() {
        assertThatThrownBy(() -> BTreePageType.fromCode(1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid code");
    }

    @Test
    void isLeafTrue() {
        assertThat(BTreePageType.fromCode(10).isLeaf()).isTrue();
        assertThat(BTreePageType.fromCode(13).isLeaf()).isTrue();
    }

    @Test
    void isLeafFalse() {
        assertThat(BTreePageType.fromCode(2).isLeaf()).isFalse();
        assertThat(BTreePageType.fromCode(5).isLeaf()).isFalse();
    }


    @Test
    void isTableTrue() {
        assertThat(BTreePageType.fromCode(5).isTable()).isTrue();
        assertThat(BTreePageType.fromCode(13).isTable()).isTrue();
    }

    @Test
    void isTableFalse() {
        assertThat(BTreePageType.fromCode(2).isTable()).isFalse();
        assertThat(BTreePageType.fromCode(10).isTable()).isFalse();
    }

    @Test
    void getHeaderSizeLeaf() {
        assertThat(BTreePageType.fromCode(10).getHeaderSize()).isEqualTo(8);
        assertThat(BTreePageType.fromCode(13).getHeaderSize()).isEqualTo(8);
    }

    @Test
    void getHeaderSizeInterior() {
        assertThat(BTreePageType.fromCode(2).getHeaderSize()).isEqualTo(12);
        assertThat(BTreePageType.fromCode(5).getHeaderSize()).isEqualTo(12);
    }
}
