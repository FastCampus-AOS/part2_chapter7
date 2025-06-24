package fastcampus.aos.part2.part2_chapter7

enum class Category(val value: String) {
    POP("POP"),    // 강수 확률
    PTY("PTY"),    // 강수 형태
    SKY("SKY"),    // 하늘 상태
    TMP("TMP");    // 1시간 기온

    companion object {
        fun from(value: String): Category? {
            return Category.entries.find { it.value == value }
        }
    }
}