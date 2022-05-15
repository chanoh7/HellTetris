package hell.tetris;

public class ResourceID {
    //TODO https://ddolcat.tistory.com/540
    //난이도 버튼 그림
    public static final int[] LEVEL = {
            R.drawable.easy,
            R.drawable.normal,
            R.drawable.hard,
            R.drawable.extreme,
            R.drawable.hell
    };

    //숫자 그림
    public static final int[] DIGIT = {
            R.drawable.digit0,
            R.drawable.digit1,
            R.drawable.digit2,
            R.drawable.digit3,
            R.drawable.digit4,
            R.drawable.digit5,
            R.drawable.digit6,
            R.drawable.digit7,
            R.drawable.digit8,
            R.drawable.digit9
    };

    //블록 색
    private static final int[] BLOCK_COLOR = {
            //공백. 블록 지울 때 사용
            R.drawable.blank_dot,

            //1번 ~ 10번 블록
            R.drawable.c01,
            R.drawable.c02,
            R.drawable.c03,
            R.drawable.c04,
            R.drawable.c05,
            R.drawable.c06,
            R.drawable.c07,
            R.drawable.c08,
            R.drawable.c09,
            R.drawable.c10,

            //11번 ~ 20번 블록
            R.drawable.c11,
            R.drawable.c12,
            R.drawable.c13,
            R.drawable.c14,
            R.drawable.c15,
            R.drawable.c16,
            R.drawable.c17,
            R.drawable.c18,
            R.drawable.c19,
            R.drawable.c20,

            //21번 ~
            R.drawable.c21,
            R.drawable.c22,
    };

    //클리어 메세지
    public static final int[] CLEAR_OF = {
            R.string.clear_easy,
            R.string.clear_normal,
            R.string.clear_hard,
            R.string.clear_extreme,
            R.string.clear_hell
    };

    //클리어 메세지
    public static final int[] NEXT_OF = {
            R.string.next_of_easy,
            R.string.next_of_normal,
            R.string.next_of_hard,
            R.string.next_of_extreme,
            R.string.next_of_extreme_2
    };

    //익스트림 클리어 후
    public static final int[] OVER_THE_EXTREME = {
            R.string.over_the_extreme_0,
            R.string.over_the_extreme_1,
            R.string.over_the_extreme_2,
            R.string.over_the_extreme_3,
            R.string.over_the_extreme_4,
            R.string.over_the_extreme_5,
            R.string.over_the_extreme_6,
            R.string.over_the_extreme_7,
            R.string.over_the_extreme_8,
            R.string.over_the_extreme_9,
            R.string.over_the_extreme_10,
            R.string.over_the_extreme_11
    };

    //헬 클리어 후
    public static final int[] OVER_THE_HELL = {
            R.string.over_the_hell_0,
            R.string.over_the_hell_1,
            R.string.over_the_hell_2,
            R.string.over_the_hell_3,
            R.string.over_the_hell_4,
            R.string.over_the_hell_5,
            R.string.over_the_hell_6
    };
}
