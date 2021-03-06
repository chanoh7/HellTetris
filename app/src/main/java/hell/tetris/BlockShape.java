package hell.tetris;

public class BlockShape {
    //블록 범위표
    public static final int[][][] size = {
            //회전 방향 수
            {
                    {
                            0,                //더미
                            2, 1, 4, 4, 4,    // 1번 ~  5번 블럭
                            2, 2, 2, 4, 4,    // 6번 ~ 10번 블럭
                            4, 1, 4, 4, 4,    //11번 ~ 15번 블럭
                            2, 2, 4, 4, 4,    //16번 ~ 19번 블럭
                            2, 1            //21번 ~
                    }
            },

            {{2, 4}, {4, 2}},                // 1번 ~  5번 블럭
            {{2, 2}},
            {{3, 2}, {3, 3}, {3, 3}, {2, 3}},
            {{3, 2}, {3, 3}, {3, 3}, {2, 3}},
            {{3, 3}, {3, 3}, {2, 3}, {3, 2}},

            {{3, 2}, {2, 3}},                // 6번 ~ 10번 블럭
            {{3, 2}, {2, 3}},
            {{4, 3}, {3, 4}},
            {{3, 3}, {3, 3}, {3, 3}, {3, 3}},
            {{4, 3}, {3, 4}, {3, 3}, {3, 3}},

            {{3, 3}, {4, 3}, {3, 4}, {3, 3}},//11번 ~ 15번 블럭
            {{3, 3}},
            {{3, 3}, {3, 3}, {3, 3}, {3, 3}},
            {{3, 3}, {3, 3}, {3, 3}, {3, 3}},
            {{3, 3}, {3, 3}, {3, 3}, {3, 3}},

            {{3, 3}, {3, 3}},                //16번 ~ 19번 블럭
            {{3, 3}, {3, 3}},
            {{4, 3}, {3, 4}, {4, 3}, {3, 4}},
            {{4, 3}, {3, 4}, {4, 3}, {3, 4}},
            {{3, 3}, {3, 3}, {3, 3}, {3, 3}},

            {{2, 1}, {1, 2}},                //21번 ~
            {{3, 3}}
    };

    //블록을 정의(BLOCK_TYPE종류)*(4방향)
    public static final int[][][][] shape = {
            //첫째 블럭
            {
                    {
                            {0, 1},
                            {0, 1},
                            {0, 1},
                            {0, 1}
                    },
                    {
                            {0, 0, 0, 0},
                            {1, 1, 1, 1}
                    }
            },
            //둘째 블럭
            {
                    {
                            {2, 2},
                            {2, 2}
                    }
            },
            //셋째 블럭
            {
                    {
                            {0, 3, 0},
                            {3, 3, 3}
                    },
                    {
                            {0, 3, 0},
                            {0, 3, 3},
                            {0, 3, 0}
                    },
                    {
                            {0, 0, 0},
                            {3, 3, 3},
                            {0, 3, 0}
                    },
                    {
                            {0, 3},
                            {3, 3},
                            {0, 3}
                    }
            },
            //넷째 블럭
            {
                    {
                            {4, 4, 4},
                            {0, 0, 4}
                    },
                    {
                            {0, 0, 4},
                            {0, 0, 4},
                            {0, 4, 4}
                    },
                    {
                            {0, 0, 0},
                            {4, 0, 0},
                            {4, 4, 4}
                    },
                    {
                            {4, 4},
                            {4, 0},
                            {4, 0}
                    }
            },
            //다섯째 블럭
            {
                    {
                            {0, 5, 5},
                            {0, 0, 5},
                            {0, 0, 5}
                    },
                    {
                            {0, 0, 0},
                            {0, 0, 5},
                            {5, 5, 5}
                    },
                    {
                            {5, 0},
                            {5, 0},
                            {5, 5}
                    },
                    {
                            {5, 5, 5},
                            {5, 0, 0}
                    }
            },
            //여섯째 블럭
            {
                    {
                            {0, 6, 6},
                            {6, 6, 0}
                    },
                    {
                            {6, 0},
                            {6, 6},
                            {0, 6}
                    }
            },
            //일곱째 블럭
            {
                    {
                            {7, 7, 0},
                            {0, 7, 7}
                    },
                    {
                            {0, 7},
                            {7, 7},
                            {7, 0}
                    }
            },
            //열째 블럭
            {
                    {
                            {0, 0, 0, 0},
                            {8, 8, 8, 8},
                            {8, 8, 8, 8}
                    },
                    {
                            {0, 8, 8},
                            {0, 8, 8},
                            {0, 8, 8},
                            {0, 8, 8}
                    }
            },
            //아홉째 블럭
            {
                    {
                            {9, 9, 9},
                            {9, 9, 0},
                            {9, 0, 0}
                    },
                    {
                            {9, 9, 9},
                            {0, 9, 9},
                            {0, 0, 9}
                    },
                    {
                            {0, 0, 9},
                            {0, 9, 9},
                            {9, 9, 9}
                    },
                    {
                            {9, 0, 0},
                            {9, 9, 0},
                            {9, 9, 9}
                    }
            },
            //열째 블럭
            {
                    {
                            {0, 0, 0, 0},
                            {0, 10, 10, 10},
                            {0, 10, 10, 0}
                    },
                    {
                            {0, 0, 0},
                            {0, 10, 10},
                            {0, 10, 10},
                            {0, 0, 10}
                    },
                    {
                            {0, 0, 0},
                            {0, 10, 10},
                            {10, 10, 10}
                    },
                    {
                            {0, 10, 0},
                            {0, 10, 10},
                            {0, 10, 10}
                    }
            },
            //열 한번째 블럭
            {
                    {
                            {0, 0, 11},
                            {0, 11, 11},
                            {0, 11, 11}
                    },
                    {
                            {0, 0, 0, 0},
                            {0, 11, 11, 0},
                            {0, 11, 11, 11}
                    },
                    {
                            {0, 0, 0},
                            {0, 11, 11},
                            {0, 11, 11},
                            {0, 11, 0}
                    },
                    {
                            {0, 0, 0},
                            {11, 11, 11},
                            {0, 11, 11}
                    }
            },
            //열 두번째 블럭
            {
                    {
                            {12, 12, 12},
                            {12, 12, 12},
                            {12, 12, 12},
                    }
            },
            //열 세번째 블럭
            {
                    {
                            {13, 13, 13},
                            {0, 13, 0},
                            {0, 13, 0}
                    },
                    {
                            {0, 0, 13},
                            {13, 13, 13},
                            {0, 0, 13}
                    },
                    {
                            {0, 13, 0},
                            {0, 13, 0},
                            {13, 13, 13}
                    },
                    {
                            {13, 0, 0},
                            {13, 13, 13},
                            {13, 0, 0}
                    }
            },
            //열 네번째 블럭
            {
                    {
                            {0, 14, 0},
                            {14, 14, 14},
                            {0, 0, 14}
                    },
                    {
                            {0, 14, 0},
                            {0, 14, 14},
                            {14, 14, 0}
                    },
                    {
                            {14, 0, 0},
                            {14, 14, 14},
                            {0, 14, 0}
                    },
                    {
                            {0, 14, 14},
                            {14, 14, 0},
                            {0, 14, 0}
                    }
            },
            //열 다섯번째 블럭
            {
                    {
                            {0, 15, 0},
                            {15, 15, 15},
                            {15, 0, 0}
                    },
                    {
                            {15, 15, 0},
                            {0, 15, 15},
                            {0, 15, 0}
                    },
                    {
                            {0, 0, 15},
                            {15, 15, 15},
                            {0, 15, 0}
                    },
                    {
                            {0, 15, 0},
                            {15, 15, 0},
                            {0, 15, 15}
                    }
            },
            //열 여섯번째 블럭
            {
                    {
                            {16, 16, 0},
                            {0, 16, 0},
                            {0, 16, 16}
                    },
                    {
                            {0, 0, 16},
                            {16, 16, 16},
                            {16, 0, 0}
                    }
            },
            //열 일곱번째 블럭
            {
                    {
                            {0, 17, 17},
                            {0, 17, 0},
                            {17, 17, 0}
                    },
                    {
                            {17, 0, 0},
                            {17, 17, 17},
                            {0, 0, 17}
                    }
            },
            //열 여덟번째 블럭
            {
                    {
                            {0, 0, 0, 0},
                            {18, 18, 18, 18},
                            {0, 0, 0, 18}
                    },
                    {
                            {0, 0, 18},
                            {0, 0, 18},
                            {0, 0, 18},
                            {0, 18, 18}
                    },
                    {
                            {0, 0, 0, 0},
                            {18, 0, 0, 0},
                            {18, 18, 18, 18}
                    },
                    {
                            {0, 18, 18},
                            {0, 18, 0},
                            {0, 18, 0},
                            {0, 18, 0}
                    }
            },
            //열 아홉번째 블럭
            {
                    {
                            {0, 0, 0, 0},
                            {19, 19, 19, 19},
                            {19, 0, 0, 0}
                    },
                    {
                            {0, 19, 19},
                            {0, 0, 19},
                            {0, 0, 19},
                            {0, 0, 19}
                    },
                    {
                            {0, 0, 0, 0},
                            {0, 0, 0, 19},
                            {19, 19, 19, 19}
                    },
                    {
                            {0, 19, 0},
                            {0, 19, 0},
                            {0, 19, 0},
                            {0, 19, 19}
                    }
            },
            //스무 번째 블럭
            {
                    {
                            {20, 20, 20},
                            {0, 0, 20},
                            {0, 0, 20}
                    },
                    {
                            {0, 0, 20},
                            {0, 0, 20},
                            {20, 20, 20}
                    },
                    {
                            {20, 0, 0},
                            {20, 0, 0},
                            {20, 20, 20}
                    },
                    {
                            {20, 20, 20},
                            {20, 0, 0},
                            {20, 0, 0}
                    }
            },
            //스물 한번째 블럭
            {
                    {
                            {21, 21}
                    },
                    {
                            {21},
                            {21}
                    }
            },
            //스물 두번째 블럭
            {
                    {
                            {0, 22, 0},
                            {22, 22, 22},
                            {0, 22, 0}
                    }
            }
    };

    //셧다운 메시지
    public static final int[][] SHUTDOWN_ARY = {
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},    //첫 줄 공백
            {0, 0, 4, 4, 0, 0, 4, 4, 0, 0},    //하트
            {0, 4, 4, 4, 4, 4, 4, 4, 4, 0},
            {0, 4, 4, 4, 4, 4, 4, 4, 4, 0},
            {0, 0, 4, 4, 4, 4, 4, 4, 0, 0},
            {0, 0, 0, 4, 4, 4, 4, 0, 0, 0},
            {0, 0, 0, 0, 4, 4, 0, 0, 0, 0},    //하트
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},    //공백
            {0, 0, 2, 2, 0, 0, 5, 0, 8, 0},    //잉!
            {0, 2, 0, 0, 2, 0, 5, 0, 8, 0},
            {0, 2, 0, 0, 2, 0, 5, 0, 8, 0},
            {0, 0, 2, 2, 0, 0, 5, 0, 8, 0},
            {0, 0, 0, 0, 7, 7, 0, 0, 8, 0},
            {0, 0, 0, 7, 0, 0, 7, 0, 0, 0},
            {0, 0, 0, 7, 0, 0, 7, 0, 8, 0},
            {0, 0, 0, 0, 7, 7, 0, 0, 0, 0},    //잉!
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0},    //공백
            {0, 0, 3, 3, 0, 0, 4, 0, 2, 0},    //여!
            {0, 3, 0, 0, 3, 4, 4, 0, 2, 0},
            {0, 3, 0, 0, 3, 0, 4, 0, 2, 0},
            {0, 3, 0, 0, 3, 4, 4, 0, 2, 0},
            {0, 0, 3, 3, 0, 0, 4, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 4, 0, 2, 0},    //여!
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 0}    //공백
    };

    public static final int[][] THANKS_FOR_PLAY = {
            {8, 8, 8, 8, 8, 0, 0, 0, 0, 4},
            {0, 0, 8, 0, 8, 0, 0, 0, 0, 4},
            {0, 0, 8, 0, 8, 4, 4, 4, 4, 4},
            {0, 3, 3, 0, 0, 0, 0, 0, 0, 4},
            {3, 0, 0, 3, 0, 0, 0, 0, 0, 4},
            {0, 3, 3, 0, 0, 2, 2, 2, 2, 2},
            {0, 0, 0, 15, 0, 0, 0, 2, 0, 0},
            {15, 15, 15, 0, 0, 0, 0, 2, 0, 0},
            {0, 0, 15, 15, 0, 2, 2, 2, 2, 2},
            {0, 0, 0, 15, 0, 10, 10, 10, 10, 0},
            {2, 2, 2, 2, 2, 0, 0, 10, 0, 10},
            {0, 0, 2, 0, 2, 0, 0, 10, 0, 10},
            {0, 0, 2, 2, 2, 10, 10, 10, 10, 0},
            {0, 0, 0, 0, 0, 18, 18, 18, 18, 18},
            {8, 8, 8, 8, 8, 0, 0, 0, 18, 0},
            {0, 0, 0, 0, 0, 0, 0, 18, 0, 0},
            {0, 4, 4, 0, 4, 18, 18, 18, 18, 18},
            {4, 0, 4, 0, 4, 9, 9, 9, 9, 9},
            {0, 4, 4, 4, 0, 0, 0, 9, 0, 0},
            {4, 0, 0, 0, 7, 0, 9, 0, 9, 0},
            {0, 0, 0, 7, 0, 9, 0, 0, 0, 9},
            {7, 7, 7, 0, 0, 11, 0, 11, 11, 11},
            {0, 0, 0, 7, 0, 11, 0, 11, 0, 11},
            {0, 0, 0, 0, 7, 11, 11, 11, 0, 11}
    };

    // 블록의 가로세로를 리턴
    public static int[] getSize(int n, int r) {
        return size[n][r];
    }

    //1번 회전시켰을 때의 가로세로를 리턴
    public static int[] getNextSize(int n, int r) {
        return size[n][(r + 1) % size[0][0][n]];
    }

    //1번 회전시킨 모양을 리턴
    public static int[][] getNextShape(int n, int r) {
        return shape[n - 1][(r + 1) % size[0][0][n]];
    }

    //블록 번호를 받고, 그 블록의 회전 방향 종류를 리턴
    public static int getNumOfRotation(int n) {
        return size[0][0][n];
    }
}
