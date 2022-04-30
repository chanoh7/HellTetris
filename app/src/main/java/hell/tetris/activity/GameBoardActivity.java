package hell.tetris.activity;

import android.widget.*;
import android.app.*;
import android.os.*;
import android.view.*;

import java.util.concurrent.atomic.AtomicBoolean;

import hell.tetris.BlockShape;
import hell.tetris.R;
import hell.tetris.ResourceID;

enum ActiveState {STATE_READY, STATE_RUN, STATE_PAUSE, STATE_END}

enum GamePhase {PHASE_NEXT_BLOCK, PHASE_BLOCK_DOWN, PHASE_LINE_SET, PHASE_LINE_ERASE, PHASE_END}

enum GameType {
    EASY(5),
    NORMAL(7),
    HARD(9),
    EXTREME(22),
    HELL(EXTREME.value - HARD.value);

    private final int value;

    GameType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    GameType next() {
        return GameType.values()[(this.ordinal() + 1) % GameType.values().length];
    }
}

public class GameBoardActivity extends Activity {

    private static final int MAX_LINE = 24;         //화면 층 수
    private static final int BLOCK_IN_LINE = 10;    //한 줄 블록 수

    private static final int STARTING_X = 3;    //소환 위치
    private static final int X = 0;
    private static final int Y = 1;

    private static final int DIA_EXIT = 0;      //종료 대화상자
    private static final int MSG_RUN = 0;       //타이머 메시지

    private static final int CMD_LEFT = 1;      //명령 번호
    private static final int CMD_RIGHT = 2;
    private static final int CMD_DOWN = 3;
    private static final int CMD_DROP = 4;
    private static final int CMD_ROTATE = 5;

    private static final int CMD_MOVE_RANGE = 20;    //명령인식 정밀도 TODO 화면 크기에 따라 가변 값으로 수정
    private static final int CMD_DROP_RANGE = 90;
    private static final int CMD_ROTATE_RANGE = 30;

    private static final int MAX_LEVEL = 15;        //만렙(블록 낙하 속도)
    private static final int INITIAL_SPEED = 500;   //렙1 때 낙하속도
    private static final int SPEEDUP = 25;          //가속량
    private static final int[] MAX_EXP =            //렙업 필요 경험치
            {0,
                    3000, 5000, 7000, 9000, 12000,    //Lv 1~ 5
                    15000, 18000, 21000, 24000, 27000,    //Lv 6~10
                    30000, 30000, 30000, 30000, 30000,    //Lv11~15
            };
    private static final int OVER_EXP = 35000;
    private static final int MAX_SCORE = 999999;

    private static final int EXP_SINGLE_BLOCK = 10;    //점수
    private static final int EXP_SINGLE_LINE = 300;
    private static final int EXP_DOWN = 5;
    private static final int EXP_DROP = 10;
    private static final int EXP_PER_COMBO = 500;

    private static final int BTN_RESET = R.drawable.reset;
    private static final int BTN_PAUSE = R.drawable.pause;
    private static final int BTN_GO = R.drawable.go;
    private static final int BTN_SURPRISE = R.drawable.suprise;
    public static final int PREVIEW_WINDOW_SIZE = 4;

    ImageView[][] Blocks;   //블록 쌓이는 필드
    ImageView[][] Preview;  //다음 블록 미리보기 창
    ImageView[] levelView;  //현재 레벨 표시 뷰
    ImageView[] scoreView;  //점수 표시 뷰

    int[][] blockAry;       //블록 필드 색칠 상태
    int[] lineWeight;       //각 층의 블록 수
    int[][] currentBlock;   //낙하중인 블록
    int[][] nextBlock;      //다음에 나올 블록
    int[] blockSize;        //블록 크기

    int blockNumber;        //현재 블록 번호
    int blockRotation;      //현재 블록 방향
    int nextBlockNumber;    //다음 블록 번호
    int nextBlockRotation;  //다음 블록 방향
    int x;                  //현재 블록 x좌표
    int y;                  //현재 블록 y좌표

    int level;      //난이도
    int score;      //점수
    int speed;      //낙하 속도
    int exp;        //경험치통
    int multi;      //여러 줄 깰 때 추가 경험치
    int combo;      //연속으로 깰 때 추가 경험치
    int dropHeight; //강제낙하 높이

    // 터치 이벤트 관련
    float touchX, touchY;
    boolean singleMotion;
    boolean clearExtreme = false;
    boolean clearHell = false;

    // 입력, 자동진행 상호배제용
    int what;
    AtomicBoolean blocked = new AtomicBoolean();
    AtomicBoolean busy = new AtomicBoolean();

    ActiveState state;  //상태(준비, 실행중, 일시정지, 종료)
    GameType gameType;  //난이도(쉬움, 보통, 어려움, 극한, 지옥)
    GamePhase phase;    //블록 처리 단계(다음 거 추출, 하강, 착지, 줄 삭제, 마무리)

    Handler timer = new Handler() {
        public void handleMessage(Message msg) {
            run();
        }
    };

    //생성자
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gameboard);

        SetBlocksView();
        SetPreview();
        setScoreAndLevelView();

        blockAry = new int[MAX_LINE + 2][BLOCK_IN_LINE];
        lineWeight = new int[MAX_LINE + 2];
        for (int i = 0; i < BLOCK_IN_LINE; i++) {
            blockAry[0][i] = -1;
        }

        gameType = GameType.EASY;
        resetData();
        state = ActiveState.STATE_READY;
    }

    //일시 이탈 시 정지
    public void onPause() {
        super.onPause();
        if (state == ActiveState.STATE_RUN) {
            state = ActiveState.STATE_PAUSE;
            ((ImageView) findViewById(R.id.stateButton)).setImageResource(BTN_GO);
        }
    }

    //백 버튼 반응
    public boolean onKeyDown(int KeyCode, KeyEvent event) {
        switch (KeyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (state == ActiveState.STATE_RUN)
                    state = ActiveState.STATE_PAUSE;

                showDialog(DIA_EXIT);
                break;

            default:
                return false;//시스템에게 (나머지) 처리를 넘긴다
        }

        return true;//내가 다 처리했다
    }

    //터치 이벤트로 명령 입력
    public boolean onTouchEvent(MotionEvent event) {
        if (dropHeight > 0 || state != ActiveState.STATE_RUN)
            return true;//내가 다 처리했다

        switch (event.getAction()) {
            //초기 터치 위치 기록
            case MotionEvent.ACTION_DOWN:
                touchX = event.getX();
                touchY = event.getY();
                break;

            //드래그 시, 초기 위치에서 이동량 계산(x,y축 따로)
            case MotionEvent.ACTION_MOVE:
                float cmpX = event.getX() - touchX;
                float cmpY = event.getY() - touchY;

                //드래그 방향에 따라 명령 종류를 결정하고 처리 함수 호출
                //처리하면서 현위치를 초기 위치로 설정
                // 세로 방향 모션이 더 클 때
                if (cmpY * cmpY > cmpX * cmpX) {
                    //회전
                    if (singleMotion && cmpY < -CMD_ROTATE_RANGE) {
                        what = CMD_ROTATE;
                        touchX = event.getX();
                        touchY = event.getY();
                        singleMotion = false;
                        run();
                    }
                    // 떨구기
                    else if (y < MAX_LINE && singleMotion && cmpY > CMD_DROP_RANGE) {
                        what = CMD_DROP;
                        //강제낙하시킨 높이도 기록
                        dropHeight = y;
                        touchX = event.getX();
                        touchY = event.getY();
                        singleMotion = false;
                        run();
                    }
                    // 내리기
                    else if (cmpY > CMD_MOVE_RANGE) {
                        what = CMD_DOWN;
                        touchX = event.getX();
                        touchY = event.getY();
                        run();
                    }
                }
                //가로 모션이 더 클 때
                else {
                    //오른쪽 이동
                    if (cmpX > CMD_MOVE_RANGE) {
                        what = CMD_RIGHT;
                        touchX = event.getX();
                        touchY = event.getY();
                        run();
                    }
                    //왼쪽 이동
                    else if (cmpX < -CMD_MOVE_RANGE) {
                        what = CMD_LEFT;
                        touchX = event.getX();
                        touchY = event.getY();
                        run();
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                singleMotion = true;
                break;

            default:
                return false;//시스템에게 넘긴다.
        }
        return true;//내가 다 처리했다
    }

    //대화상자 미리 생성
    protected Dialog onCreateDialog(int id) {
        //백 버튼 눌렀을 때, 종료 할까, 말까? 대화상자
        if (id == DIA_EXIT) {
            return new AlertDialog.Builder(this)
                    .setTitle(R.string.exit_title)
                    .setMessage(R.string.exit_message)
                    .setPositiveButton(R.string.button_ok, (dialog, whichButton) -> finish())
                    .setNegativeButton(R.string.button_cancel, null)
                    .create();
        }

        return null;
    }

    //시작, 일시정지 버튼
    public void changeState(View v) {
        switch (state) {
            case STATE_READY:
                ((ImageView) findViewById(R.id.levelbutton)).setClickable(false);
                state = ActiveState.STATE_RUN;
                ((ImageView) v).setImageResource(BTN_PAUSE);
                run();
                break;

            case STATE_RUN:
                state = ActiveState.STATE_PAUSE;
                ((ImageView) v).setImageResource(BTN_GO);
                break;

            case STATE_PAUSE:
                state = ActiveState.STATE_RUN;
                ((ImageView) v).setImageResource(BTN_PAUSE);
                run();
                break;

            case STATE_END:
                if (clearExtreme)
                    esterEggExtreme(0);
                else if (clearHell)
                    esterEggHell(0);
                else {
                    resetWithScreen();
                    state = ActiveState.STATE_READY;
                    ((ImageView) v).setImageResource(BTN_GO);
                }
                break;
        }
    }

    //난이도 변경
    public void changeLevel(View v) {
        gameType = gameType.next();
        ((ImageView) v).setImageResource(ResourceID.LEVEL[gameType.ordinal()]);

        makeNextBlock();
    }

    //블록 뷰와 코드를 연결
    private void SetBlocksView() {
        Blocks = new ImageView[MAX_LINE + 1][BLOCK_IN_LINE];

        Blocks[0][0] = (ImageView) findViewById(R.id.block00_0);
        Blocks[0][1] = (ImageView) findViewById(R.id.block00_1);
        Blocks[0][2] = (ImageView) findViewById(R.id.block00_2);
        Blocks[0][3] = (ImageView) findViewById(R.id.block00_3);
        Blocks[0][4] = (ImageView) findViewById(R.id.block00_4);
        Blocks[0][5] = (ImageView) findViewById(R.id.block00_5);
        Blocks[0][6] = (ImageView) findViewById(R.id.block00_6);
        Blocks[0][7] = (ImageView) findViewById(R.id.block00_7);
        Blocks[0][8] = (ImageView) findViewById(R.id.block00_8);
        Blocks[0][9] = (ImageView) findViewById(R.id.block00_9);

        Blocks[1][0] = (ImageView) findViewById(R.id.block01_0);
        Blocks[1][1] = (ImageView) findViewById(R.id.block01_1);
        Blocks[1][2] = (ImageView) findViewById(R.id.block01_2);
        Blocks[1][3] = (ImageView) findViewById(R.id.block01_3);
        Blocks[1][4] = (ImageView) findViewById(R.id.block01_4);
        Blocks[1][5] = (ImageView) findViewById(R.id.block01_5);
        Blocks[1][6] = (ImageView) findViewById(R.id.block01_6);
        Blocks[1][7] = (ImageView) findViewById(R.id.block01_7);
        Blocks[1][8] = (ImageView) findViewById(R.id.block01_8);
        Blocks[1][9] = (ImageView) findViewById(R.id.block01_9);

        Blocks[2][0] = (ImageView) findViewById(R.id.block02_0);
        Blocks[2][1] = (ImageView) findViewById(R.id.block02_1);
        Blocks[2][2] = (ImageView) findViewById(R.id.block02_2);
        Blocks[2][3] = (ImageView) findViewById(R.id.block02_3);
        Blocks[2][4] = (ImageView) findViewById(R.id.block02_4);
        Blocks[2][5] = (ImageView) findViewById(R.id.block02_5);
        Blocks[2][6] = (ImageView) findViewById(R.id.block02_6);
        Blocks[2][7] = (ImageView) findViewById(R.id.block02_7);
        Blocks[2][8] = (ImageView) findViewById(R.id.block02_8);
        Blocks[2][9] = (ImageView) findViewById(R.id.block02_9);

        Blocks[3][0] = (ImageView) findViewById(R.id.block03_0);
        Blocks[3][1] = (ImageView) findViewById(R.id.block03_1);
        Blocks[3][2] = (ImageView) findViewById(R.id.block03_2);
        Blocks[3][3] = (ImageView) findViewById(R.id.block03_3);
        Blocks[3][4] = (ImageView) findViewById(R.id.block03_4);
        Blocks[3][5] = (ImageView) findViewById(R.id.block03_5);
        Blocks[3][6] = (ImageView) findViewById(R.id.block03_6);
        Blocks[3][7] = (ImageView) findViewById(R.id.block03_7);
        Blocks[3][8] = (ImageView) findViewById(R.id.block03_8);
        Blocks[3][9] = (ImageView) findViewById(R.id.block03_9);

        Blocks[4][0] = (ImageView) findViewById(R.id.block04_0);
        Blocks[4][1] = (ImageView) findViewById(R.id.block04_1);
        Blocks[4][2] = (ImageView) findViewById(R.id.block04_2);
        Blocks[4][3] = (ImageView) findViewById(R.id.block04_3);
        Blocks[4][4] = (ImageView) findViewById(R.id.block04_4);
        Blocks[4][5] = (ImageView) findViewById(R.id.block04_5);
        Blocks[4][6] = (ImageView) findViewById(R.id.block04_6);
        Blocks[4][7] = (ImageView) findViewById(R.id.block04_7);
        Blocks[4][8] = (ImageView) findViewById(R.id.block04_8);
        Blocks[4][9] = (ImageView) findViewById(R.id.block04_9);

        Blocks[5][0] = (ImageView) findViewById(R.id.block05_0);
        Blocks[5][1] = (ImageView) findViewById(R.id.block05_1);
        Blocks[5][2] = (ImageView) findViewById(R.id.block05_2);
        Blocks[5][3] = (ImageView) findViewById(R.id.block05_3);
        Blocks[5][4] = (ImageView) findViewById(R.id.block05_4);
        Blocks[5][5] = (ImageView) findViewById(R.id.block05_5);
        Blocks[5][6] = (ImageView) findViewById(R.id.block05_6);
        Blocks[5][7] = (ImageView) findViewById(R.id.block05_7);
        Blocks[5][8] = (ImageView) findViewById(R.id.block05_8);
        Blocks[5][9] = (ImageView) findViewById(R.id.block05_9);

        Blocks[6][0] = (ImageView) findViewById(R.id.block06_0);
        Blocks[6][1] = (ImageView) findViewById(R.id.block06_1);
        Blocks[6][2] = (ImageView) findViewById(R.id.block06_2);
        Blocks[6][3] = (ImageView) findViewById(R.id.block06_3);
        Blocks[6][4] = (ImageView) findViewById(R.id.block06_4);
        Blocks[6][5] = (ImageView) findViewById(R.id.block06_5);
        Blocks[6][6] = (ImageView) findViewById(R.id.block06_6);
        Blocks[6][7] = (ImageView) findViewById(R.id.block06_7);
        Blocks[6][8] = (ImageView) findViewById(R.id.block06_8);
        Blocks[6][9] = (ImageView) findViewById(R.id.block06_9);

        Blocks[7][0] = (ImageView) findViewById(R.id.block07_0);
        Blocks[7][1] = (ImageView) findViewById(R.id.block07_1);
        Blocks[7][2] = (ImageView) findViewById(R.id.block07_2);
        Blocks[7][3] = (ImageView) findViewById(R.id.block07_3);
        Blocks[7][4] = (ImageView) findViewById(R.id.block07_4);
        Blocks[7][5] = (ImageView) findViewById(R.id.block07_5);
        Blocks[7][6] = (ImageView) findViewById(R.id.block07_6);
        Blocks[7][7] = (ImageView) findViewById(R.id.block07_7);
        Blocks[7][8] = (ImageView) findViewById(R.id.block07_8);
        Blocks[7][9] = (ImageView) findViewById(R.id.block07_9);

        Blocks[8][0] = (ImageView) findViewById(R.id.block08_0);
        Blocks[8][1] = (ImageView) findViewById(R.id.block08_1);
        Blocks[8][2] = (ImageView) findViewById(R.id.block08_2);
        Blocks[8][3] = (ImageView) findViewById(R.id.block08_3);
        Blocks[8][4] = (ImageView) findViewById(R.id.block08_4);
        Blocks[8][5] = (ImageView) findViewById(R.id.block08_5);
        Blocks[8][6] = (ImageView) findViewById(R.id.block08_6);
        Blocks[8][7] = (ImageView) findViewById(R.id.block08_7);
        Blocks[8][8] = (ImageView) findViewById(R.id.block08_8);
        Blocks[8][9] = (ImageView) findViewById(R.id.block08_9);

        Blocks[9][0] = (ImageView) findViewById(R.id.block09_0);
        Blocks[9][1] = (ImageView) findViewById(R.id.block09_1);
        Blocks[9][2] = (ImageView) findViewById(R.id.block09_2);
        Blocks[9][3] = (ImageView) findViewById(R.id.block09_3);
        Blocks[9][4] = (ImageView) findViewById(R.id.block09_4);
        Blocks[9][5] = (ImageView) findViewById(R.id.block09_5);
        Blocks[9][6] = (ImageView) findViewById(R.id.block09_6);
        Blocks[9][7] = (ImageView) findViewById(R.id.block09_7);
        Blocks[9][8] = (ImageView) findViewById(R.id.block09_8);
        Blocks[9][9] = (ImageView) findViewById(R.id.block09_9);

        Blocks[10][0] = (ImageView) findViewById(R.id.block10_0);
        Blocks[10][1] = (ImageView) findViewById(R.id.block10_1);
        Blocks[10][2] = (ImageView) findViewById(R.id.block10_2);
        Blocks[10][3] = (ImageView) findViewById(R.id.block10_3);
        Blocks[10][4] = (ImageView) findViewById(R.id.block10_4);
        Blocks[10][5] = (ImageView) findViewById(R.id.block10_5);
        Blocks[10][6] = (ImageView) findViewById(R.id.block10_6);
        Blocks[10][7] = (ImageView) findViewById(R.id.block10_7);
        Blocks[10][8] = (ImageView) findViewById(R.id.block10_8);
        Blocks[10][9] = (ImageView) findViewById(R.id.block10_9);

        Blocks[11][0] = (ImageView) findViewById(R.id.block11_0);
        Blocks[11][1] = (ImageView) findViewById(R.id.block11_1);
        Blocks[11][2] = (ImageView) findViewById(R.id.block11_2);
        Blocks[11][3] = (ImageView) findViewById(R.id.block11_3);
        Blocks[11][4] = (ImageView) findViewById(R.id.block11_4);
        Blocks[11][5] = (ImageView) findViewById(R.id.block11_5);
        Blocks[11][6] = (ImageView) findViewById(R.id.block11_6);
        Blocks[11][7] = (ImageView) findViewById(R.id.block11_7);
        Blocks[11][8] = (ImageView) findViewById(R.id.block11_8);
        Blocks[11][9] = (ImageView) findViewById(R.id.block11_9);

        Blocks[12][0] = (ImageView) findViewById(R.id.block12_0);
        Blocks[12][1] = (ImageView) findViewById(R.id.block12_1);
        Blocks[12][2] = (ImageView) findViewById(R.id.block12_2);
        Blocks[12][3] = (ImageView) findViewById(R.id.block12_3);
        Blocks[12][4] = (ImageView) findViewById(R.id.block12_4);
        Blocks[12][5] = (ImageView) findViewById(R.id.block12_5);
        Blocks[12][6] = (ImageView) findViewById(R.id.block12_6);
        Blocks[12][7] = (ImageView) findViewById(R.id.block12_7);
        Blocks[12][8] = (ImageView) findViewById(R.id.block12_8);
        Blocks[12][9] = (ImageView) findViewById(R.id.block12_9);

        Blocks[13][0] = (ImageView) findViewById(R.id.block13_0);
        Blocks[13][1] = (ImageView) findViewById(R.id.block13_1);
        Blocks[13][2] = (ImageView) findViewById(R.id.block13_2);
        Blocks[13][3] = (ImageView) findViewById(R.id.block13_3);
        Blocks[13][4] = (ImageView) findViewById(R.id.block13_4);
        Blocks[13][5] = (ImageView) findViewById(R.id.block13_5);
        Blocks[13][6] = (ImageView) findViewById(R.id.block13_6);
        Blocks[13][7] = (ImageView) findViewById(R.id.block13_7);
        Blocks[13][8] = (ImageView) findViewById(R.id.block13_8);
        Blocks[13][9] = (ImageView) findViewById(R.id.block13_9);

        Blocks[14][0] = (ImageView) findViewById(R.id.block14_0);
        Blocks[14][1] = (ImageView) findViewById(R.id.block14_1);
        Blocks[14][2] = (ImageView) findViewById(R.id.block14_2);
        Blocks[14][3] = (ImageView) findViewById(R.id.block14_3);
        Blocks[14][4] = (ImageView) findViewById(R.id.block14_4);
        Blocks[14][5] = (ImageView) findViewById(R.id.block14_5);
        Blocks[14][6] = (ImageView) findViewById(R.id.block14_6);
        Blocks[14][7] = (ImageView) findViewById(R.id.block14_7);
        Blocks[14][8] = (ImageView) findViewById(R.id.block14_8);
        Blocks[14][9] = (ImageView) findViewById(R.id.block14_9);

        Blocks[15][0] = (ImageView) findViewById(R.id.block15_0);
        Blocks[15][1] = (ImageView) findViewById(R.id.block15_1);
        Blocks[15][2] = (ImageView) findViewById(R.id.block15_2);
        Blocks[15][3] = (ImageView) findViewById(R.id.block15_3);
        Blocks[15][4] = (ImageView) findViewById(R.id.block15_4);
        Blocks[15][5] = (ImageView) findViewById(R.id.block15_5);
        Blocks[15][6] = (ImageView) findViewById(R.id.block15_6);
        Blocks[15][7] = (ImageView) findViewById(R.id.block15_7);
        Blocks[15][8] = (ImageView) findViewById(R.id.block15_8);
        Blocks[15][9] = (ImageView) findViewById(R.id.block15_9);

        Blocks[16][0] = (ImageView) findViewById(R.id.block16_0);
        Blocks[16][1] = (ImageView) findViewById(R.id.block16_1);
        Blocks[16][2] = (ImageView) findViewById(R.id.block16_2);
        Blocks[16][3] = (ImageView) findViewById(R.id.block16_3);
        Blocks[16][4] = (ImageView) findViewById(R.id.block16_4);
        Blocks[16][5] = (ImageView) findViewById(R.id.block16_5);
        Blocks[16][6] = (ImageView) findViewById(R.id.block16_6);
        Blocks[16][7] = (ImageView) findViewById(R.id.block16_7);
        Blocks[16][8] = (ImageView) findViewById(R.id.block16_8);
        Blocks[16][9] = (ImageView) findViewById(R.id.block16_9);

        Blocks[17][0] = (ImageView) findViewById(R.id.block17_0);
        Blocks[17][1] = (ImageView) findViewById(R.id.block17_1);
        Blocks[17][2] = (ImageView) findViewById(R.id.block17_2);
        Blocks[17][3] = (ImageView) findViewById(R.id.block17_3);
        Blocks[17][4] = (ImageView) findViewById(R.id.block17_4);
        Blocks[17][5] = (ImageView) findViewById(R.id.block17_5);
        Blocks[17][6] = (ImageView) findViewById(R.id.block17_6);
        Blocks[17][7] = (ImageView) findViewById(R.id.block17_7);
        Blocks[17][8] = (ImageView) findViewById(R.id.block17_8);
        Blocks[17][9] = (ImageView) findViewById(R.id.block17_9);

        Blocks[18][0] = (ImageView) findViewById(R.id.block18_0);
        Blocks[18][1] = (ImageView) findViewById(R.id.block18_1);
        Blocks[18][2] = (ImageView) findViewById(R.id.block18_2);
        Blocks[18][3] = (ImageView) findViewById(R.id.block18_3);
        Blocks[18][4] = (ImageView) findViewById(R.id.block18_4);
        Blocks[18][5] = (ImageView) findViewById(R.id.block18_5);
        Blocks[18][6] = (ImageView) findViewById(R.id.block18_6);
        Blocks[18][7] = (ImageView) findViewById(R.id.block18_7);
        Blocks[18][8] = (ImageView) findViewById(R.id.block18_8);
        Blocks[18][9] = (ImageView) findViewById(R.id.block18_9);

        Blocks[19][0] = (ImageView) findViewById(R.id.block19_0);
        Blocks[19][1] = (ImageView) findViewById(R.id.block19_1);
        Blocks[19][2] = (ImageView) findViewById(R.id.block19_2);
        Blocks[19][3] = (ImageView) findViewById(R.id.block19_3);
        Blocks[19][4] = (ImageView) findViewById(R.id.block19_4);
        Blocks[19][5] = (ImageView) findViewById(R.id.block19_5);
        Blocks[19][6] = (ImageView) findViewById(R.id.block19_6);
        Blocks[19][7] = (ImageView) findViewById(R.id.block19_7);
        Blocks[19][8] = (ImageView) findViewById(R.id.block19_8);
        Blocks[19][9] = (ImageView) findViewById(R.id.block19_9);

        Blocks[20][0] = (ImageView) findViewById(R.id.block20_0);
        Blocks[20][1] = (ImageView) findViewById(R.id.block20_1);
        Blocks[20][2] = (ImageView) findViewById(R.id.block20_2);
        Blocks[20][3] = (ImageView) findViewById(R.id.block20_3);
        Blocks[20][4] = (ImageView) findViewById(R.id.block20_4);
        Blocks[20][5] = (ImageView) findViewById(R.id.block20_5);
        Blocks[20][6] = (ImageView) findViewById(R.id.block20_6);
        Blocks[20][7] = (ImageView) findViewById(R.id.block20_7);
        Blocks[20][8] = (ImageView) findViewById(R.id.block20_8);
        Blocks[20][9] = (ImageView) findViewById(R.id.block20_9);

        Blocks[21][0] = (ImageView) findViewById(R.id.block21_0);
        Blocks[21][1] = (ImageView) findViewById(R.id.block21_1);
        Blocks[21][2] = (ImageView) findViewById(R.id.block21_2);
        Blocks[21][3] = (ImageView) findViewById(R.id.block21_3);
        Blocks[21][4] = (ImageView) findViewById(R.id.block21_4);
        Blocks[21][5] = (ImageView) findViewById(R.id.block21_5);
        Blocks[21][6] = (ImageView) findViewById(R.id.block21_6);
        Blocks[21][7] = (ImageView) findViewById(R.id.block21_7);
        Blocks[21][8] = (ImageView) findViewById(R.id.block21_8);
        Blocks[21][9] = (ImageView) findViewById(R.id.block21_9);

        Blocks[22][0] = (ImageView) findViewById(R.id.block22_0);
        Blocks[22][1] = (ImageView) findViewById(R.id.block22_1);
        Blocks[22][2] = (ImageView) findViewById(R.id.block22_2);
        Blocks[22][3] = (ImageView) findViewById(R.id.block22_3);
        Blocks[22][4] = (ImageView) findViewById(R.id.block22_4);
        Blocks[22][5] = (ImageView) findViewById(R.id.block22_5);
        Blocks[22][6] = (ImageView) findViewById(R.id.block22_6);
        Blocks[22][7] = (ImageView) findViewById(R.id.block22_7);
        Blocks[22][8] = (ImageView) findViewById(R.id.block22_8);
        Blocks[22][9] = (ImageView) findViewById(R.id.block22_9);

        Blocks[23][0] = (ImageView) findViewById(R.id.block23_0);
        Blocks[23][1] = (ImageView) findViewById(R.id.block23_1);
        Blocks[23][2] = (ImageView) findViewById(R.id.block23_2);
        Blocks[23][3] = (ImageView) findViewById(R.id.block23_3);
        Blocks[23][4] = (ImageView) findViewById(R.id.block23_4);
        Blocks[23][5] = (ImageView) findViewById(R.id.block23_5);
        Blocks[23][6] = (ImageView) findViewById(R.id.block23_6);
        Blocks[23][7] = (ImageView) findViewById(R.id.block23_7);
        Blocks[23][8] = (ImageView) findViewById(R.id.block23_8);
        Blocks[23][9] = (ImageView) findViewById(R.id.block23_9);

        Blocks[24][0] = (ImageView) findViewById(R.id.block24_0);
        Blocks[24][1] = (ImageView) findViewById(R.id.block24_1);
        Blocks[24][2] = (ImageView) findViewById(R.id.block24_2);
        Blocks[24][3] = (ImageView) findViewById(R.id.block24_3);
        Blocks[24][4] = (ImageView) findViewById(R.id.block24_4);
        Blocks[24][5] = (ImageView) findViewById(R.id.block24_5);
        Blocks[24][6] = (ImageView) findViewById(R.id.block24_6);
        Blocks[24][7] = (ImageView) findViewById(R.id.block24_7);
        Blocks[24][8] = (ImageView) findViewById(R.id.block24_8);
        Blocks[24][9] = (ImageView) findViewById(R.id.block24_9);
    }

    //미리보기 뷰와 코드를 연결
    private void SetPreview() {
        Preview = new ImageView[4][4];

        Preview[0][0] = (ImageView) findViewById(R.id.block_n0_0);
        Preview[0][1] = (ImageView) findViewById(R.id.block_n0_1);
        Preview[0][2] = (ImageView) findViewById(R.id.block_n0_2);
        Preview[0][3] = (ImageView) findViewById(R.id.block_n0_3);

        Preview[1][0] = (ImageView) findViewById(R.id.block_n1_0);
        Preview[1][1] = (ImageView) findViewById(R.id.block_n1_1);
        Preview[1][2] = (ImageView) findViewById(R.id.block_n1_2);
        Preview[1][3] = (ImageView) findViewById(R.id.block_n1_3);

        Preview[2][0] = (ImageView) findViewById(R.id.block_n2_0);
        Preview[2][1] = (ImageView) findViewById(R.id.block_n2_1);
        Preview[2][2] = (ImageView) findViewById(R.id.block_n2_2);
        Preview[2][3] = (ImageView) findViewById(R.id.block_n2_3);

        Preview[3][0] = (ImageView) findViewById(R.id.block_n3_0);
        Preview[3][1] = (ImageView) findViewById(R.id.block_n3_1);
        Preview[3][2] = (ImageView) findViewById(R.id.block_n3_2);
        Preview[3][3] = (ImageView) findViewById(R.id.block_n3_3);
    }

    //점수/레벨 표시 뷰 연결
    private void setScoreAndLevelView() {
        levelView = new ImageView[2];
        levelView[0] = (ImageView) findViewById(R.id.level0);
        levelView[1] = (ImageView) findViewById(R.id.level1);

        scoreView = new ImageView[6];
        scoreView[0] = (ImageView) findViewById(R.id.score0);
        scoreView[1] = (ImageView) findViewById(R.id.score1);
        scoreView[2] = (ImageView) findViewById(R.id.score2);
        scoreView[3] = (ImageView) findViewById(R.id.score3);
        scoreView[4] = (ImageView) findViewById(R.id.score4);
        scoreView[5] = (ImageView) findViewById(R.id.score5);
    }

    //화면 빼고 초기화
    private void resetData() {
        level = score = 0;
        updateLevel(true);

        ((ImageView) findViewById(R.id.levelbutton)).setClickable(true);
        ((ImageView) findViewById(R.id.levelbutton)).setImageResource(ResourceID.LEVEL[gameType.ordinal()]);

        what = 0;
        speed = INITIAL_SPEED;
        singleMotion = true;

        makeNextBlock();

        phase = GamePhase.PHASE_NEXT_BLOCK;
        state = ActiveState.STATE_READY;
    }

    //화면까지 초기화
    private void resetWithScreen() {
        for (int i = 1; i <= MAX_LINE + 1; i++) {
            lineWeight[i] = 0;

            for (int j = 0; j < BLOCK_IN_LINE; j++) {
                blockAry[i][j] = 0;
                setOneBlock(i - 1, j, 0);
            }
        }

        resetData();
    }

    //메인 루틴.
    //블록 낙하 또는 사용자 입력에 따른 명령을 처리
    private void run() {
        //락을 획득하고 실행
        if (busy.getAndSet(true)) {
            //락을 놓친 경우, 밀린 일이 있다고 기록
            blocked.getAndSet(true);
            return;
        }

        if (what == 0)
            blockDropFlow();
        else
            command();

        //락 해제
        busy.getAndSet(false);

        //밀린 일이 있으면 실행
        if (blocked.get()) {
            blocked.getAndSet(false);
            timer.sendEmptyMessage(MSG_RUN);
        }
    }

    //블록 낙하 루틴
    private void blockDropFlow() {
        boolean auto = true;

        while (auto && state == ActiveState.STATE_RUN)
            switch (phase) {
                case PHASE_NEXT_BLOCK:
                    /* 다음 블록 소환.
                     * 성공하면 잠시 후 한 줄 내린다.
                     * 실패하면 게임오버로 갈 것이다. */
                    if (nextBlock()) {
                        auto = false;
                        timer.sendEmptyMessageDelayed(MSG_RUN, speed);
                    }
                    break;

                case PHASE_BLOCK_DOWN:
                    /* 한 줄 내리기.
                     * 성공하면 잠시 후 또 내린다.
                     * 실패하면 현재 줄 제거 판정으로 갈 것이다. */
                    blockDown();
                    auto = false;
                    timer.sendEmptyMessageDelayed(MSG_RUN, speed);
                    break;

                case PHASE_LINE_SET:
                    /* 블록이 바닥에 도착.
                     * 줄 제거하러 가거나, 다음 블럭 소환하러 감.
                     * 바닥 달리기로 빠져나가면 낙하 루틴 속행*/
                    if (!lineSet()) {
                        auto = false;
                        timer.sendEmptyMessageDelayed(MSG_RUN, speed);
                    }
                    break;

                case PHASE_LINE_ERASE:
                    /* 한 줄이 꽉 참.
                     * 꽉 찬 줄을 제거하고, 다음 블럭 소환하러 감. */
                    clearFilledLine();
                    break;

                case PHASE_END:
                    state = ActiveState.STATE_END;
                    ((ImageView) findViewById(R.id.stateButton)).setImageResource(BTN_RESET);
                    break;
            }
    }

    //다음 블록 부르기
    //성공 여부를 리턴
    private boolean nextBlock() {
        currentBlock = nextBlock;
        blockNumber = nextBlockNumber;
        blockRotation = nextBlockRotation;
        blockSize = BlockShape.getSize(blockNumber, blockRotation);

        //다음 블록 추출
        makeNextBlock();

        //다음 블록 소환
        return summonBlock();
    }

    //다음 블록 추출
    private void makeNextBlock() {
        nextBlockNumber = (int) (Math.random() * gameType.getValue()) + 1;

        //HELL은 HARD까지 나오던 블록들이 안 나오므로 그 숫자만큼 뒤로 밀어낸다
        if (gameType == GameType.HELL) {
            nextBlockNumber += GameType.HARD.getValue();
        }

        nextBlockRotation = (int) (Math.random() * BlockShape.size[0][0][nextBlockNumber]);
        nextBlock = BlockShape.shape[nextBlockNumber - 1][nextBlockRotation];

        updatePreview();
    }

    //미리보기 창 갱신
    private void updatePreview() {
        int[] sizeNext = BlockShape.getSize(nextBlockNumber, nextBlockRotation);

        for (int i = 0; i < PREVIEW_WINDOW_SIZE; i++)
            for (int j = 0; j < PREVIEW_WINDOW_SIZE; j++)
                if (i < sizeNext[Y] && j < sizeNext[X])
                    Preview[i][j].setImageResource(ResourceID.BLOCK_COLOR[nextBlock[i][j]]);
                else
                    Preview[i][j].setImageResource(ResourceID.BLOCK_COLOR[0]);
    }

    //다음 블록을 화면에 소환.
    //성공 여부를 리턴
    private boolean summonBlock() {
        //강제 낙하 높이 초기화
        dropHeight = 0;

        //이 위치에 소환한다.
        y = MAX_LINE + 1;
        x = STARTING_X + (int) (Math.random() * 2);

        // 나올 공간이 없으면 게임 끝
        if (isOverlap()) {
            phase = GamePhase.PHASE_END;
            return false;
        }

        // 공간이 있으면 화면에 등장.
        drawBlock();

        //다음에 할 일은 한 줄 내리기
        phase = GamePhase.PHASE_BLOCK_DOWN;
        return true;
    }

    //블록 한 줄 내리기
    //성공 여부를 리턴
    private boolean blockDown() {
        //걸리는 게 없으면 한 줄 내림
        if (isDownable()) {
            //원래 있던 자리에서 지우기
            eraseBlock();

            //한 칸 내리고
            y--;

            //새로운 위치에 그리기
            drawBlock();

            return true;
        }
        //웜가 걸리면 멈춤
        else {
            phase = GamePhase.PHASE_LINE_SET;
            return false;
        }
    }

    //아래쪽 충돌검사
    private boolean isDownable() {
        int i, j;
        boolean drop = true;

        //현재 블록위치에서 한 칸 아래에 걸리는 게 있는지 검사
        for (j = 0; j < blockSize[X] && drop; j++) {
            i = blockSize[Y];
            while (i > 0 && currentBlock[i - 1][j] == 0) {
                i--;
            }

            if (i > 0 && blockAry[y - i][x + j] != 0)
                drop = false;
        }

        return drop;
    }

    //블록이 바닥에 도착.
    private boolean lineSet() {
        //바닥 슬라이드로 빠져나가면 낙하 판정 속행
        if (blockDown()) {
            phase = GamePhase.PHASE_BLOCK_DOWN;
            return false;
        }

        boolean clear = false;

        //이번 블록이 멈춘 줄들을 검사하자
        for (int i = 0; i < blockSize[Y]; i++) {
            //블록이 새로 생긴 층은 블록 카운트 증가
            for (int j = 0; j < blockSize[X]; j++)
                if (currentBlock[i][j] != 0) {
                    lineWeight[y - i]++;

                    //블록의 덩치에 비례한 득점
                    score += EXP_SINGLE_BLOCK;
                    exp += EXP_SINGLE_BLOCK;
                }

            //한 줄이 꽉 차면 지울 블록이 생겼다고 표시
            if (lineWeight[y - i] == BLOCK_IN_LINE)
                clear = true;
        }

        //줄 삭제 페이즈로 갈 경우 거기서 점수 갱신을 할 것이고, 그렇지 않은 경우 여기서 해야 한다.
        if (clear) {
            phase = GamePhase.PHASE_LINE_ERASE;
            combo++;
        } else {
            updateScore();
            phase = GamePhase.PHASE_NEXT_BLOCK;
            combo = 0;
        }

        return true;
    }

    //블록이 떨어진 곳에서 꽉 찬 줄 찾아서 제거
    private void clearFilledLine() {
        //방금 떨어진 블록의 바닥 줄
        int i = y - blockSize[Y] + 1;

        //줄 수 득점보정 초기화
        multi = 0;

        //지금 떨어진 블록이 차지한 층을 검사하여 꽉 찬 줄 제거
        for (; i <= y; i++) {
            if (lineWeight[i] == BLOCK_IN_LINE) {
                //줄 수 득점보정
                multi++;

                //한 줄 제거
                clearTargetLine(i);

                //한 줄 사라졌으니 보정한다.
                i--;

                //점수도 오른다.
                exp += EXP_SINGLE_LINE * multi * combo;
                score += EXP_SINGLE_LINE * multi * combo;
            }
        }

        //콤보 점수는 따로 오른다.
        if (combo > 1) {
            exp += (combo - 1) * EXP_PER_COMBO;
            score += (combo - 1) * EXP_PER_COMBO;
        }

        updateScore();

        phase = GamePhase.PHASE_NEXT_BLOCK;
    }

    //한 줄 제거 함수
    private void clearTargetLine(int l) {
        int k;

        //윗줄 전체를(빈 줄이 아닐 때만) 아래로 당긴다.
        for (k = l; lineWeight[k + 1] > 0 && k < MAX_LINE; k++) {
            lineWeight[k] = lineWeight[k + 1];

            for (int j = 0; j < BLOCK_IN_LINE; j++) {
                blockAry[k][j] = blockAry[k + 1][j];
                setOneBlock(k - 1, j, blockAry[k][j]);
            }
        }

        lineWeight[k] = lineWeight[k + 1];

        for (int j = 0; j < BLOCK_IN_LINE; j++) {
            blockAry[k][j] = 0;
            setOneBlock(k - 1, j, 0);
        }

        //도착한 블록의 높이 정보를 갱신한다.
        y--;

        //한 줄 이상 내려앉았으니, 맷 윗 줄은 빈 줄이다.
        lineWeight[MAX_LINE + 1] = 0;
    }

    //점수 갱신
    public void updateScore() {
        //최고점 돌파하면 그만
        if (score > MAX_SCORE)
            gameClear();
        else {
            if (level < MAX_LEVEL) {
                if (exp > MAX_EXP[level])
                    updateLevel(true);
            } else if (exp > OVER_EXP)
                updateLevel(false);
        }

        //점수판 각 자릿수에 맞는 숫자 출력
        int scale = 1;
        for (ImageView scoreDigit : scoreView) {
            scoreDigit.setImageResource(ResourceID.DIGIT[(score / scale) % 10]);
            scale *= 10;
        }
    }

    // 레벨 변경 관련
    private void updateLevel(boolean isLvUp) {
        int bonusDel;
        exp = 0;

        if (isLvUp) {
            level++;
            levelView[0].setImageResource(ResourceID.DIGIT[level % 10]);
            levelView[1].setImageResource(ResourceID.DIGIT[level / 10]);
            speed -= SPEEDUP;
            bonusDel = level;
        } else
            bonusDel = (int) (Math.random() * 6) + 5;

        //렙업 점수
        multi = 0;
        combo++;
        for (int i = 1; lineWeight[1] > 0 && i < bonusDel; i++) {
            multi++;

            clearTargetLine(1);

            score += EXP_SINGLE_LINE * multi * combo;
        }

        updateScore();
    }

    //사용자의 조작을 처리하는 함수.
    private void command() {
        switch (what) {
            case CMD_LEFT:
                moveLeft();
                break;

            case CMD_RIGHT:
                moveRight();
                break;

            case CMD_DOWN:
                blockDown();
                exp += EXP_DOWN;
                break;

            case CMD_DROP:
                eraseBlock();
                while (isDownable())
                    y--;
                drawBlock();

                exp += (dropHeight - y) * EXP_DROP;
                score += (dropHeight - y) * EXP_DROP;

                lineSet();
                break;

            //회전 명령은, 현재 블록이 회전 가능한 블록일 때만 호출
            case CMD_ROTATE:
                if (BlockShape.getNumOfRotation(blockNumber) > 1)
                    rotate();
                break;
        }

        //입력받은 명령을 처리했으니 낙하 루틴으로 복귀하자
        what = 0;
    }

    //왼쪽으로 1칸
    private void moveLeft() {
        int moved_x = x - 1;
        int i, j;
        boolean movable = true;

        //왼쪽으로 화면 이탈 검사
        if (moved_x == -2) return;
        else if (moved_x == -1) {
            for (i = 0; i < blockSize[Y] && currentBlock[i][0] == 0; i++)
                ;

            if (i < blockSize[Y])
                return;
        }

        //왼쪽에 장애물 검사
        for (i = 0; i < blockSize[Y]; i++) {
            for (j = 0; j < blockSize[X] && currentBlock[i][j] == 0; j++)
                ;

            if (j < blockSize[X] && blockAry[y - i][moved_x + j] != 0)
                movable = false;
        }

        if (movable) {
            //현재 블록을 지우고
            eraseBlock();

            //위치 재설정
            x--;

            //다시 그린다.
            drawBlock();
        }
    }

    //오른쪽으로 1칸
    private void moveRight() {
        int moved_x = x + 1;
        int i, j;
        boolean movable = true;

        //오른쪽으로 화면 이탈 검사
        if (moved_x + blockSize[X] > BLOCK_IN_LINE)
            return;

        //오른쪽에 장애물 검사
        for (i = 0; i < blockSize[Y]; i++) {
            for (j = blockSize[X]; j > 0 && currentBlock[i][j - 1] == 0; j--)
                ;

            if (j > 0 && blockAry[y - i][x + j] != 0)
                movable = false;
        }

        if (movable) {
            //현재 블록을 지우고
            eraseBlock();

            //위치 재설정
            x++;

            //다시 그린다.
            drawBlock();
        }
    }

    //회전
    private void rotate() {
        int nextSize[] = BlockShape.getNextSize(blockNumber, blockRotation);
        int nextX = x;

        //지하 뚫게 생기면 회전 안 함
        if (y - nextSize[Y] < 1) return;

        //블록 정보를 저장한 뒤
        int[][] tempBlock = currentBlock;
        int[] tempSize = blockSize;
        int tempRotation = blockRotation;
        int tempX = x;

        //가로축 방향으로 화면 나가게 생기면 안쪽으로 끌어와서 판정
        if (x < 0) nextX = 0;
        else {
            int j = x + nextSize[X];
            while (j > BLOCK_IN_LINE) {
                j--;
                nextX--;
            }
        }

        //현재 블록을 지우고
        eraseBlock();

        //새로운 위치에
        currentBlock = BlockShape.getNextShape(blockNumber, blockRotation);
        blockSize = nextSize;
        blockRotation = (blockRotation + 1) % BlockShape.getNumOfRotation(blockNumber);
        x = nextX;

        //겹치는 게 있나 판정
        if (isOverlap()) {
            currentBlock = tempBlock;    //겹치면, 원위치
            blockSize = tempSize;
            blockRotation = tempRotation;
            x = tempX;
        }

        //다시 그린다.
        drawBlock();
    }

    //블록이 겹치는지 조사
    private boolean isOverlap() {
        for (int i = 0; i < blockSize[Y]; i++)
            for (int j = 0; j < blockSize[X]; j++)
                if (currentBlock[i][j] != 0 && blockAry[y - i][x + j] != 0)
                    return true;

        return false;
    }

    //낙하중인 블록을 현 위치에서 지운다.
    private void eraseBlock() {
        for (int i = 0; i < blockSize[Y]; i++)
            for (int j = 0; j < blockSize[X]; j++)
                if (currentBlock[i][j] != 0) {
                    blockAry[y - i][x + j] = 0;
                    Blocks[y - i - 1][x + j].setImageResource(ResourceID.BLOCK_COLOR[0]);
                }
    }

    //현재 낙하중인 블록을 그린다.
    private void drawBlock() {
        for (int i = 0; i < blockSize[Y]; i++)
            for (int j = 0; j < blockSize[X]; j++)
                if (currentBlock[i][j] != 0) {
                    blockAry[y - i][x + j] = blockNumber;
                    setOneBlock(y - i - 1, x + j, blockNumber);
                }
    }

    //주어진 위치에 지정 블록 채우기
    private void setOneBlock(int height, int block, int color) {
        Blocks[height][block].setImageResource(ResourceID.BLOCK_COLOR[color]);
    }

    //만점 돌파하면 종료
    private void gameClear() {
        score = MAX_SCORE;

        state = ActiveState.STATE_END;
        ((ImageView) findViewById(R.id.stateButton)).setImageResource(BTN_RESET);

        switch (gameType) {
            case EASY:
            case NORMAL:
            case HARD:
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(ResourceID.CLEAR_OF[gameType.ordinal()])
                        .setMessage(ResourceID.NEXT_OF[gameType.ordinal()])
                        .setPositiveButton(R.string.button_ok, null)
                        .show();

                gameType = gameType.next();
                break;

            case EXTREME:
                clearExtreme = true;

                blockAry = BlockShape.SHUTDOWN_ARY;
                for (int i = 1; i <= MAX_LINE; i++)
                    for (int j = 0; j < BLOCK_IN_LINE; j++)
                        setOneBlock(i - 1, j, blockAry[MAX_LINE - i][j]);

                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(R.string.clear_extreme)
                        .setMessage(R.string.next_of_extreme_2)
                        .setPositiveButton(R.string.button_ok, null)
                        .show();

                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(R.string.clear_extreme)
                        .setMessage(R.string.next_of_extreme)
                        .setPositiveButton(R.string.button_ok, null)
                        .show();
                break;

            case HELL:
                clearHell = true;
                ((ImageView) findViewById(R.id.stateButton)).setImageResource(BTN_SURPRISE);

                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(R.string.clear_hell)
                        .setPositiveButton(R.string.button_ok, null)
                        .show();
                break;
        }
    }

    //Extreme 클리어 메시지를 순서대로 띄움
    //마지막 두 문장은 같이 나옴
    private void esterEggExtreme(int messageIndex) {
        if (messageIndex < ResourceID.OVER_THE_EXTREME.length - 2) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(ResourceID.OVER_THE_EXTREME[messageIndex])
                    .setPositiveButton(
                            R.string.button_ok,
                            (dialog, whichButton) -> esterEggExtreme(messageIndex + 1)
                    )
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(ResourceID.OVER_THE_EXTREME[messageIndex])
                    .setMessage(ResourceID.OVER_THE_EXTREME[messageIndex + 1])
                    .show();
        }
    }

    //Hell 클리어 메시지를 순서대로 띄움
    private void esterEggHell(int messageIndex) {
        if (messageIndex < ResourceID.OVER_THE_HELL.length) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(ResourceID.OVER_THE_HELL[messageIndex])
                    .setPositiveButton(
                            R.string.button_ok,
                            (dialog, whichButton) -> esterEggHell(messageIndex + 1)
                    )
                    .show();
        } else {
//            message++;//TODO ???
            blockAry = BlockShape.THANKS_FOR_PLAY;
            for (int i = 1; i <= MAX_LINE; i++)
                for (int j = 0; j < BLOCK_IN_LINE; j++)
                    setOneBlock(i - 1, j, blockAry[MAX_LINE - i][j]);
        }
    }
}