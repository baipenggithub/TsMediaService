package com.ts.service.media.utils;

import androidx.annotation.Nullable;

import com.ts.sdk.media.bean.AudioInfoBean;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SortUtils {
    private static final String TAG = "SortUtils";
    private static final String NUMBER_REGEX = "[0-9]+";
    private static final String LETTER_REGEX = "[a-zA-Z]+";
    private static final int LESS_THAN = -1;
    private static final int GREATER_THAN = 1;
    private static final int DEF_EQUAL = 0;

    private SortUtils() {
        // Do nothing.
    }

    /**
     * Sort contacts.
     *
     * @param beanList beanList
     */
    public static void sortAudioListData(@Nullable List<AudioInfoBean> beanList) {
        if (Objects.equals(null, beanList)) {
            return;
        }
        final ArrayList<AudioInfoBean> letterList = new ArrayList<>();
        final ArrayList<AudioInfoBean> numberList = new ArrayList<>();
        final ArrayList<AudioInfoBean> otherList = new ArrayList<>();
        for (AudioInfoBean bean : beanList) {
            if (Objects.equals(null, bean.getAudioName())) {
                otherList.add(bean);
            } else if (matchNumberFirst(bean.getAudioName())) {
                numberList.add(bean);
            } else if (matchLetterFirst(bean.getAudioName())) {
                letterList.add(bean);
            } else {
                otherList.add(bean);
            }
        }
        letterList.sort(Comparator.comparing(obj -> obj.getAudioName().trim(),
                new LetterComparator()));
        numberList.sort(Comparator.comparing(obj -> obj.getAudioName().trim()));
        otherList.sort(Comparator.comparing(obj -> obj.getAudioName().trim(),
                new SortVideoName()));
        beanList.clear();
        beanList.addAll(letterList);
        beanList.addAll(numberList);
        beanList.addAll(otherList);
    }

    private static boolean matchLetterFirst(String input) {
        Pattern pattern = Pattern.compile(LETTER_REGEX);
        Matcher matcher = pattern.matcher(input);
        return matcher.lookingAt();
    }

    private static boolean matchNumberFirst(String input) {
        Pattern pattern = Pattern.compile(NUMBER_REGEX);
        Matcher matcher = pattern.matcher(input);
        return matcher.lookingAt();
    }

    /**
     * To ==> [a,A,b,B,c,C]. Not [a,b,c,d,A,B,C,D].
     */
    private static class LetterComparator
            implements Comparator<String> {

        private static final int FACTOR = 10;
        private static final int DIFF = FACTOR / 2;

        @Override
        public int compare(String name1, String name2) {
            int length1 = name1.length();
            int length2 = name2.length();
            int min = Math.min(length1, length2);
            for (int i = 0; i < min; i++) {
                char nameCharAt1 = name1.charAt(i);
                char nameCharAt2 = name2.charAt(i);
                boolean isUpper1 = Character.isUpperCase(nameCharAt1);
                boolean isUpper2 = Character.isUpperCase(nameCharAt2);
                if (nameCharAt1 != nameCharAt2) {
                    // No overflow because of numeric promotion
                    boolean allUpper = isUpper1 && isUpper2;
                    boolean allLower = !isUpper1 && !isUpper2;
                    if (allUpper || allLower) {
                        return FACTOR * (nameCharAt1 - nameCharAt2);
                    } else if (isUpper1) {
                        return FACTOR * (Character.toLowerCase(nameCharAt1) - nameCharAt2) + DIFF;
                    } else {
                        return FACTOR * (nameCharAt1 - Character.toLowerCase(nameCharAt2)) - DIFF;
                    }
                }
            }
            return length1 - length2;
        }
    }

    /**
     * Chinese alphabetical order.
     */
    private static class SortVideoName implements Comparator<String> {

        Collator cmp = Collator.getInstance(java.util.Locale.CHINA);

        @Override
        public int compare(String videoName1, String videoName2) {
            if (cmp.compare(videoName1, videoName2) > DEF_EQUAL) {
                return GREATER_THAN;
            } else if (cmp.compare(videoName1, videoName2) < DEF_EQUAL) {
                return LESS_THAN;
            }
            return DEF_EQUAL;
        }
    }
}
