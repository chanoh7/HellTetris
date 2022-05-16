package hell.tetris.activity;

import android.content.res.TypedArray;
import android.util.DisplayMetrics;
import android.widget.*;
import android.app.*;
import android.os.*;
import android.view.*;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.databinding.DataBindingUtil;
import hell.tetris.BlockShape;
import hell.tetris.R;
import hell.tetris.databinding.ActivityGameboardBinding;

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

    private static final int MAX_LINE = 24;             //화면 층 수
    private static final int BLOCK_IN_LINE = 10;        //한 줄 블록 수
    private static final int PREVIEW_WINDOW_SIZE = 4;   //미리보기 창 크기

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
    private static final int BTN_RESUME = R.drawable.resume;
    private static final int BTN_SURPRISE = R.drawable.suprise;

    ImageView[][] blockField;   //블록 쌓이는 필드
    ImageView[][] previewBlock; //다음 블록 미리보기 창
    ImageView[] levelView;      //현재 레벨 표시 뷰
    ImageView[] scoreView;      //점수 표시 뷰

    TypedArray levelImgAry; //난이도 그림 id
    TypedArray digitImgAry; //숫자 그림 id
    int[] blockColorAry;    //블록 색
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
    int dropHeight; //강제낙하가 실행된 높이

    // 터치 이벤트 관련
    float touchX, touchY;
    boolean singleMotion;

    int cmdMoveRange;   //명령인식 정밀도 (화면 크기에 따라 가변 값)
    int cmdDropRange;
    int cmdRotateRange;

    //이스터 에그 플래그
    boolean clearExtreme = false;
    boolean clearHell = false;

    //난이도 별 최고점수 찍었을 때 메시지
    String[] overTheHellMsg;
    String[] overTheExtremeMsg;
    String[] clearOf;
    String[] nextOf;

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

    ActivityGameboardBinding binding;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_gameboard);

        init();
        initBlockField();
        initGameInfo();
        resetData();
    }

    //일시 이탈 시 정지
    public void onPause() {
        super.onPause();
        if (state == ActiveState.STATE_RUN) {
            state = ActiveState.STATE_PAUSE;
            binding.btnState.setImageResource(BTN_RESUME);
        }
    }

    //백 버튼 반응
    public boolean onKeyDown(int KeyCode, KeyEvent event) {
        switch (KeyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (state == ActiveState.STATE_RUN) {
                    state = ActiveState.STATE_PAUSE;
                    binding.btnState.setImageResource(BTN_RESUME);
                }

                showDialog(DIA_EXIT);
                break;

            default:
                //시스템에게 (나머지) 처리를 넘긴다
                return false;
        }

        //내가 다 처리했다
        return true;
    }

    //터치 이벤트로 명령 입력
    public boolean onTouchEvent(MotionEvent event) {
        if (dropHeight > 0 || state != ActiveState.STATE_RUN) {
            //내가 다 처리했다
            return true;
        }

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
                    if (singleMotion && cmpY < -cmdRotateRange) {
                        what = CMD_ROTATE;
                        touchX = event.getX();
                        touchY = event.getY();
                        singleMotion = false;
                        run();
                    }
                    // 떨구기
                    else if (y < MAX_LINE && singleMotion && cmpY > cmdDropRange) {
                        what = CMD_DROP;
                        //강제낙하시킨 높이도 기록
                        dropHeight = y;
                        touchX = event.getX();
                        touchY = event.getY();
                        singleMotion = false;
                        run();
                    }
                    // 내리기
                    else if (cmpY > cmdMoveRange) {
                        what = CMD_DOWN;
                        touchX = event.getX();
                        touchY = event.getY();
                        run();
                    }
                }
                //가로 모션이 더 클 때
                else {
                    //오른쪽 이동
                    if (cmpX > cmdMoveRange) {
                        what = CMD_RIGHT;
                        touchX = event.getX();
                        touchY = event.getY();
                        run();
                    }
                    //왼쪽 이동
                    else if (cmpX < -cmdMoveRange) {
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
                //시스템에게 넘긴다.
                return false;
        }
        //내가 다 처리했다
        return true;
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
    public void onClickState(View v) {
        switch (state) {
            case STATE_READY:
                binding.btnLevel.setClickable(false);
                state = ActiveState.STATE_RUN;
                binding.btnState.setImageResource(BTN_PAUSE);
                run();
                break;

            case STATE_RUN:
                state = ActiveState.STATE_PAUSE;
                binding.btnState.setImageResource(BTN_RESUME);
                break;

            case STATE_PAUSE:
                state = ActiveState.STATE_RUN;
                binding.btnState.setImageResource(BTN_PAUSE);
                run();
                break;

            case STATE_END:
                if (clearExtreme) {
                    esterEggExtreme(0);
                } else if (clearHell) {
                    esterEggHell(0);
                } else {
                    resetWithScreen();
                    state = ActiveState.STATE_READY;
                    binding.btnState.setImageResource(BTN_RESUME);
                }
                break;
        }
    }

    //난이도 변경
    public void onClickLevel(View v) {
        gameType = gameType.next();
        setImage((ImageView) v, levelImgAry, gameType.ordinal());

        makeNextBlock();
    }

    private void init() {
        blockColorAry = getResources().getIntArray(R.array.block_color);
        digitImgAry = getResources().obtainTypedArray(R.array.digit);
        levelImgAry = getResources().obtainTypedArray(R.array.level);

        //명령 인식 정밀도 설정(화면 크기 반영)
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        cmdMoveRange = screenWidth / 50;
        cmdDropRange = Integer.min(cmdMoveRange * 4, 200);
        cmdRotateRange = Integer.min((int) (cmdMoveRange * 1.5), 40);

        //충돌 판정을 쉽게 하기 위해, 화면 밑(밖)에 지하 1층을 만든다.
        lineWeight = new int[MAX_LINE + 2];
        blockAry = new int[MAX_LINE + 2][BLOCK_IN_LINE];
        for (int i = 0; i < BLOCK_IN_LINE; i++) {
            blockAry[0][i] = -1;
        }

        gameType = GameType.NORMAL;
        state = ActiveState.STATE_READY;
    }

    //블록 필드 생성
    private void initBlockField() {
        blockField = new ImageView[MAX_LINE + 1][BLOCK_IN_LINE];

        //블록 한 층 레이아웃 param(style.blockLine 참조)
        LinearLayout.LayoutParams blockLineParam = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        blockLineParam.setMargins(0, dp2px(1), 0, 0);

        //블록 한 개 레이아웃 param(style.emptyBlock 참조)
        LinearLayout.LayoutParams blockParam = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
        blockParam.setMargins(0, 0, dp2px(1), 0);

        //화면에 보일 층 MAX_LINE 개 추가
        for (int i = 0; i < MAX_LINE; i++) {
            LinearLayout blockLine = new LinearLayout(this);
            blockLine.setLayoutParams(blockLineParam);

            //한 줄당 BLOCK_IN_LINE 개만큼 블록 추가하고 블록 배열에 저장
            for (int j = 0; j < BLOCK_IN_LINE; j++) {
                ImageView block = new ImageView(this);
                block.setBackgroundColor(blockColorAry[0]);
                block.setLayoutParams(blockParam);
                blockLine.addView(block);
                blockField[i][j] = block;
            }

            binding.layoutBlockField.addView(blockLine, 0);
        }

        //화면 위에 데이터가 한 줄 더 있는데 예외처리 하기 귀찮으니 더미 뷰 붙임
        for (int j = 0; j < BLOCK_IN_LINE; j++) {
            blockField[MAX_LINE][j] = new ImageView(this);
        }
    }

    //미리보기, 점수, 레벨 뷰 초기화
    private void initGameInfo() {
        previewBlock = new ImageView[PREVIEW_WINDOW_SIZE][PREVIEW_WINDOW_SIZE];

        //여백 맞추느라 위, 앞에 더미 뷰 하나씩 있어서 인덱스에 +1씩 함.
        for (int i = 0; i < PREVIEW_WINDOW_SIZE; i++) {
            LinearLayout line = (LinearLayout) binding.layoutPreview.getChildAt(i + 1);
            for (int j = 0; j < PREVIEW_WINDOW_SIZE; j++) {
                previewBlock[i][j] = (ImageView) line.getChildAt(j + 1);
            }
        }

        //점수 뷰
        int scoreViewCount = binding.layoutScore.getChildCount();
        scoreView = new ImageView[scoreViewCount];
        for (int i = 0; i < scoreViewCount; i++) {
            scoreView[i] = (ImageView) binding.layoutScore.getChildAt(i);
        }

        //레벨 뷰
        int levelViewCount = binding.layoutLevel.getChildCount();
        levelView = new ImageView[levelViewCount];
        for (int i = 0; i < levelViewCount; i++) {
            levelView[i] = (ImageView) binding.layoutLevel.getChildAt(i);
        }
    }

    //화면 빼고 초기화
    private void resetData() {
        level = score = 0;
        updateLevel(true);

        binding.btnLevel.setClickable(true);
        setImage(binding.btnLevel, levelImgAry, gameType.ordinal());

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

        if (what == 0) {
            blockDropFlow();
        } else {
            command();
        }

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
                    clearFilledLines();
                    break;

                case PHASE_END:
                    state = ActiveState.STATE_END;
                    binding.btnState.setImageResource(BTN_RESET);
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

        for (int i = 0; i < PREVIEW_WINDOW_SIZE; i++) {
            for (int j = 0; j < PREVIEW_WINDOW_SIZE; j++) {
                if (i < sizeNext[Y] && j < sizeNext[X]) {
                    previewBlock[i][j].setBackgroundColor(blockColorAry[nextBlock[i][j]]);
                } else {
                    previewBlock[i][j].setBackgroundColor(blockColorAry[0]);
                }
            }
        }
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
        //어딘가에 걸리면 멈춤
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

            if (i > 0 && blockAry[y - i][x + j] != 0) {
                drop = false;
            }
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
            for (int j = 0; j < blockSize[X]; j++) {
                if (currentBlock[i][j] != 0) {
                    lineWeight[y - i]++;

                    //블록의 덩치에 비례한 득점
                    score += EXP_SINGLE_BLOCK;
                    exp += EXP_SINGLE_BLOCK;
                }
            }

            //한 줄이 꽉 차면 지울 블록이 생겼다고 표시
            if (lineWeight[y - i] == BLOCK_IN_LINE) {
                clear = true;
            }
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
    private void clearFilledLines() {
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
        if (score > MAX_SCORE) {
            gameClear();
        } else {
            if (level < MAX_LEVEL) {
                if (exp > MAX_EXP[level]) {
                    updateLevel(true);
                }
            } else if (exp > OVER_EXP) {
                updateLevel(false);
            }
        }

        //점수판 각 자릿수에 맞는 숫자 출력
        int scale = 100000;
        for (ImageView scoreDigit : scoreView) {
            setImage(scoreDigit, digitImgAry, (score / scale) % 10);
            scale /= 10;
        }
    }

    // 레벨 변경 관련
    private void updateLevel(boolean isLvUp) {
        int bonusDel;
        exp = 0;

        if (isLvUp) {
            level++;
            setImage(levelView[0], digitImgAry, level / 10);
            setImage(levelView[1], digitImgAry, level % 10);
            speed -= SPEEDUP;
            bonusDel = level;
        } else {
            bonusDel = (int) (Math.random() * 6) + 5;
        }

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
                while (isDownable()) {
                    y--;
                }
                drawBlock();

                exp += (dropHeight - y) * EXP_DROP;
                score += (dropHeight - y) * EXP_DROP;

                lineSet();
                break;

            //회전 명령은, 현재 블록이 회전 가능한 블록일 때만 호출
            case CMD_ROTATE:
                if (BlockShape.getNumOfRotation(blockNumber) > 1) {
                    rotate();
                }
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
            for (i = 0; i < blockSize[Y] && currentBlock[i][0] == 0; i++) {
                //do nothing
            }

            if (i < blockSize[Y]) {
                return;
            }
        }

        //왼쪽에 장애물 검사
        for (i = 0; i < blockSize[Y]; i++) {
            for (j = 0; j < blockSize[X] && currentBlock[i][j] == 0; j++) {
                //do nothing
            }

            if (j < blockSize[X] && blockAry[y - i][moved_x + j] != 0) {
                movable = false;
            }
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
        if (moved_x + blockSize[X] > BLOCK_IN_LINE) {
            return;
        }

        //오른쪽에 장애물 검사
        for (i = 0; i < blockSize[Y]; i++) {
            for (j = blockSize[X]; j > 0 && currentBlock[i][j - 1] == 0; j--) {
                //do nothing
            }

            if (j > 0 && blockAry[y - i][x + j] != 0) {
                movable = false;
            }
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
        int[] nextSize = BlockShape.getNextSize(blockNumber, blockRotation);
        int nextX = x;

        //지하 뚫게 생기면 회전 안 함
        if (y - nextSize[Y] < 1) {
            return;
        }

        //블록 정보를 저장한 뒤
        int[][] tempBlock = currentBlock;
        int[] tempSize = blockSize;
        int tempRotation = blockRotation;
        int tempX = x;

        //가로축 방향으로 화면 나가게 생기면 안쪽으로 끌어와서 판정
        if (x < 0) {
            nextX = 0;
        } else {
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
        for (int i = 0; i < blockSize[Y]; i++) {
            for (int j = 0; j < blockSize[X]; j++) {
                if (currentBlock[i][j] != 0 && blockAry[y - i][x + j] != 0) {
                    return true;
                }
            }
        }

        return false;
    }

    //낙하중인 블록을 현 위치에서 지운다.
    private void eraseBlock() {
        for (int i = 0; i < blockSize[Y]; i++) {
            for (int j = 0; j < blockSize[X]; j++) {
                if (currentBlock[i][j] != 0) {
                    blockAry[y - i][x + j] = 0;
                    setOneBlock(y - i - 1, x + j, 0);
                }
            }
        }
    }

    //현재 낙하중인 블록을 그린다.
    private void drawBlock() {
        for (int i = 0; i < blockSize[Y]; i++) {
            for (int j = 0; j < blockSize[X]; j++) {
                if (currentBlock[i][j] != 0) {
                    blockAry[y - i][x + j] = blockNumber;
                    setOneBlock(y - i - 1, x + j, blockNumber);
                }
            }
        }
    }

    //주어진 위치에 지정 블록 채우기
    private void setOneBlock(int height, int x, int blockNumber) {
        blockField[height][x].setBackgroundColor(blockColorAry[blockNumber]);
    }

    //만점 돌파하면 종료
    private void gameClear() {
        score = MAX_SCORE;

        state = ActiveState.STATE_END;
        binding.btnState.setImageResource(BTN_RESET);

        clearOf = getResources().getStringArray(R.array.clear_of);
        nextOf = getResources().getStringArray(R.array.next_of);

        switch (gameType) {
            case EASY:
            case NORMAL:
            case HARD:
                new AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setTitle(clearOf[gameType.ordinal()])
                        .setMessage(nextOf[gameType.ordinal()])
                        .setPositiveButton(R.string.button_ok, null)
                        .show();

                gameType = gameType.next();
                break;

            case EXTREME:
                clearExtreme = true;

                blockAry = BlockShape.SHUTDOWN_ARY;
                for (int i = 1; i <= MAX_LINE; i++) {
                    for (int j = 0; j < BLOCK_IN_LINE; j++) {
                        setOneBlock(i - 1, j, blockAry[MAX_LINE - i][j]);
                    }
                }

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
                binding.btnState.setImageResource(BTN_SURPRISE);

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
        if (overTheExtremeMsg == null) {
            overTheExtremeMsg = getResources().getStringArray(R.array.over_the_extreme);
        }

        if (messageIndex < overTheExtremeMsg.length - 2) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(overTheExtremeMsg[messageIndex])
                    .setPositiveButton(
                            R.string.button_ok,
                            (dialog, whichButton) -> esterEggExtreme(messageIndex + 1)
                    )
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(overTheExtremeMsg[messageIndex])
                    .setMessage(overTheExtremeMsg[messageIndex + 1])
                    .show();
        }
    }

    //Hell 클리어 메시지를 순서대로 띄움
    private void esterEggHell(int messageIndex) {
        if (overTheHellMsg == null) {
            overTheHellMsg = getResources().getStringArray(R.array.over_the_hell);
        }

        if (messageIndex < overTheHellMsg.length) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(overTheHellMsg[messageIndex])
                    .setPositiveButton(
                            R.string.button_ok,
                            (dialog, whichButton) -> esterEggHell(messageIndex + 1)
                    )
                    .show();
        } else {
            blockAry = BlockShape.THANKS_FOR_PLAY;
            for (int i = 1; i <= MAX_LINE; i++) {
                for (int j = 0; j < BLOCK_IN_LINE; j++) {
                    setOneBlock(i - 1, j, blockAry[MAX_LINE - i][j]);
                }
            }
        }
    }

    private void setImage(ImageView imageView, TypedArray drawableAry, int i) {
        imageView.setImageDrawable(drawableAry.getDrawable(i));
    }

    private int dp2px(int dp) {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        float density = displayMetrics.density;
        return (int) (dp * density + 0.5);
    }
}