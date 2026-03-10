package com.cocos.tvparty;

import java.util.ArrayList;
import java.util.List;

/**
 * 内置题库。家庭场景下以轻知识、生活常识为主，降低门槛。
 */
public final class QuestionBank {
    private QuestionBank() {
        // 工具类不需要实例化。
    }

    public static List<Question> defaultQuestions() {
        List<Question> questions = new ArrayList<>();
        questions.add(new Question("番茄属于哪一类食材？", new String[]{"水果", "谷物", "肉类", "坚果"}, 0));
        questions.add(new Question("一年有几个季度？", new String[]{"2 个", "3 个", "4 个", "5 个"}, 2));
        questions.add(new Question("地球围绕哪颗恒星公转？", new String[]{"月亮", "太阳", "木星", "北极星"}, 1));
        questions.add(new Question("中国传统节日“中秋节”常见食物是？", new String[]{"粽子", "月饼", "汤圆", "青团"}, 1));
        questions.add(new Question("一天有多少小时？", new String[]{"12", "18", "24", "36"}, 2));
        questions.add(new Question("下列哪项通常用于测量温度？", new String[]{"尺子", "温度计", "秒表", "秤"}, 1));
        questions.add(new Question("彩虹通常有几种颜色？", new String[]{"5", "6", "7", "8"}, 2));
        questions.add(new Question("下面哪种动物会冬眠？", new String[]{"麻雀", "熊", "海鸥", "斑马"}, 1));
        questions.add(new Question("做米饭主要使用哪种粮食？", new String[]{"小麦", "玉米", "大米", "燕麦"}, 2));
        questions.add(new Question("交通信号灯中，表示通行的是？", new String[]{"红灯", "黄灯", "绿灯", "蓝灯"}, 2));
        questions.add(new Question("“四季如春”通常形容什么？", new String[]{"天气很冷", "天气很稳定舒适", "总是下雨", "常有台风"}, 1));
        questions.add(new Question("下列哪种设备通常用于看电视节目？", new String[]{"电饭煲", "电视机", "吹风机", "电钻"}, 1));
        questions.add(new Question("人体正常呼吸需要哪种气体？", new String[]{"氧气", "氢气", "氦气", "氮气"}, 0));
        questions.add(new Question("家庭常见垃圾分类中，纸箱通常属于？", new String[]{"有害垃圾", "厨余垃圾", "可回收物", "其他垃圾"}, 2));
        questions.add(new Question("如果下雨，通常需要带什么？", new String[]{"墨镜", "雨伞", "围巾", "羽毛球拍"}, 1));
        questions.add(new Question("下面哪项是团队合作更重要的品质？", new String[]{"互相指责", "互相配合", "单打独斗", "互不沟通"}, 1));
        return questions;
    }
}
