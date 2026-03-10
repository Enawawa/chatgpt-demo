package com.cocos.tvparty;

/**
 * 题目数据模型：题干 + 四个选项 + 正确选项下标。
 */
public final class Question {
    private final String prompt;
    private final String[] options;
    private final int correctIndex;

    public Question(String prompt, String[] options, int correctIndex) {
        if (options == null || options.length != 4) {
            throw new IllegalArgumentException("每道题必须提供 4 个选项");
        }
        if (correctIndex < 0 || correctIndex >= options.length) {
            throw new IllegalArgumentException("正确答案下标超出范围");
        }
        this.prompt = prompt;
        this.options = options.clone();
        this.correctIndex = correctIndex;
    }

    public String getPrompt() {
        return prompt;
    }

    public String[] getOptions() {
        return options.clone();
    }

    public int getCorrectIndex() {
        return correctIndex;
    }
}
