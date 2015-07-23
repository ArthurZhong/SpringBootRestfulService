package com.example.spring.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by pzhong1 on 7/6/15.
 */
public class DataExpectedBuilder implements Comparable<DataExpectedBuilder> {
    private static final Pattern TAB = Pattern.compile("\t");
    private static final Pattern EQUAL = Pattern.compile("=");
    private static final Pattern DOUBLE_UNDER_SCORE = Pattern.compile("__");
    private String peopleId;
    Map<String, List<String>> rawData = Maps.newLinkedHashMap();

    public static DataExpectedBuilder newBuilder() {
        return new DataExpectedBuilder();
    }

    public DataExpectedBuilder from(String line) {
        String[] fields = TAB.split(line);
        peopleId = fields[0];
        for (int i = 1; i < fields.length; i++) {
            nameValuePair(fields[i]);
        }
        return this.sort();
    }

    public DataExpectedBuilder nameValuePair(String nvp) {
        String[] nv = EQUAL.split(nvp);
        switch (nv.length) {
            case 2:
                nameValuePair(nv[0], nv[1]);
                break;
            case 1:
                break;

            default:
                // take the last element as value
                String value = nv[nv.length - 1];
                nameValuePair(nvp.substring(0, nvp.length() - value.length()), value);
                break;
        }
        return this;
    }

    public DataExpectedBuilder nameValuePair(String name, String value) {
        String[] values = DOUBLE_UNDER_SCORE.split(value);
        switch (values.length) {
            case 1:
                name = name.trim();
                if (!rawData.containsKey(name)) {
                    rawData.put(name, new ArrayList<String>());
                }
                rawData.get(name).add(value.trim().toLowerCase());
                break;
            default:
                for (String val : values) {
                    nameValuePair(name, val);
                }
                break;
        }
        return this;
    }

    public DataExpectedBuilder sort() {
        for (List<String> values : rawData.values()) {
            Collections.sort(values);
        }
        return this;
    }

    public DataExpectedBuilder diff(DataExpectedBuilder other) {
        DataExpectedBuilder diffBuilder = newBuilder();

        Collection<String> keysIntersection = CollectionUtils.intersection(rawData.keySet(), other.rawData.keySet());

        for (String key : keysIntersection) {
            final List<String> thisValues = getValues(key);
            final List<String> otherValues = other.getValues(key);
            if (thisValues.equals(otherValues)) {
                continue;
            }

            Collection<String> valuesIntersection = CollectionUtils.intersection(thisValues, otherValues);

            for (Object value : CollectionUtils.subtract(thisValues, valuesIntersection)) {
                diffBuilder.nameValuePair(key, "-<" + value);
            }
            for (Object value : CollectionUtils.subtract(otherValues, valuesIntersection)) {
                diffBuilder.nameValuePair(key, "+>" + value);
            }
        }

        for (Object key : CollectionUtils.subtract(this.rawData.keySet(), keysIntersection)) {
            for (String value : getValues(key)) {
                diffBuilder.nameValuePair("-<" + key, "-<" + value);
            }
        }

        for (Object key : CollectionUtils.subtract(other.rawData.keySet(), keysIntersection)) {
            for (String value : other.getValues(key)) {
                diffBuilder.nameValuePair("+>" + key, "+>" + value);
            }
        }

        return diffBuilder;
    }


    public List<String> getValues(Object name) {
        return rawData.get(name);
    }


    @Override
    public int compareTo(DataExpectedBuilder other) {
        if (this.rawData.equals(other.rawData)) {
            return 0;
        }

        if (!this.rawData.keySet().containsAll(other.rawData.keySet())) {
            return -1;
        }

        Collection<String> keysIntersection = CollectionUtils.intersection(rawData.keySet(), other.rawData.keySet());
        for (String key : keysIntersection) {
            // check the matching keys have the same attributes

            final List<String> thisValues = getValues(key);
            for (String thatValue : other.getValues(key)) {
                if (thisValues.contains(thatValue)) {
                    continue;
                }
                boolean foundSimilar = false;
                for (String value : thisValues) {
                    if (hasNonEmptySubString(value, thatValue) || hasNonEmptySubString(thatValue, value)) {
                        foundSimilar = true;
                        break;
                    }
                    if (stringSimilarity(thatValue, value) > 0.9) {
                        foundSimilar = true;
                        break;
                    }
                }
                if (!foundSimilar) {
                    return -1;
                }
            }
        }

        return 1;
    }

    private static boolean hasNonEmptySubString(String source, String target) {
        return null != source && !"".equals(target) && source.contains(target);
    }

    public static float stringSimilarity(String string1, String  string2){

        //null check:
        if(string1==null || string2==null){
            return (float) 0.5;
        }


        float score = 0;//similarity between 0 y 1.

        ArrayList<String> charactersString1 = new  ArrayList<String>();
        ArrayList<String> charactersString2 = new  ArrayList<String>();

        for(int i=0 ; i < string1.length() ; i++){
            String aCharacter = String.valueOf(string1.charAt(i));
            charactersString1.add(aCharacter);
        }

        for(int i=0 ; i < string2.length() ; i++){
            String aCharacter = String.valueOf(string2.charAt(i));
            charactersString2.add(aCharacter);
        }

        //eliminate extraneous letters.
        boolean differentSize = false;
        ArrayList<String> arrayLargo = new  ArrayList<String>();
        ArrayList<String> arrayCorto = new  ArrayList<String>();
        if(charactersString1.size() < charactersString2.size()){
            arrayLargo = charactersString2;
            arrayCorto = charactersString1;
            differentSize = true;
        }else{
            if (charactersString2.size() < charactersString1.size()) {
                arrayLargo = charactersString1;
                arrayCorto = charactersString2;
                differentSize = true;

            }else{//they are the same size: yeah easy!
                if (charactersString2.size() == charactersString1.size()) {
                    for (int i=0 ; i < charactersString1.size() ; i++) {

                        String elementoS1 = charactersString1.get(i);
                        String elementoS2 = charactersString2.get(i);

                        if (elementoS1.equalsIgnoreCase(elementoS2) ) {
                            score=score+1;
                        }else{//if the elements are different.
                            if (0<i) {//if i-1 exists (ie not at the first letter).

                                String elementoS1Past = charactersString1.get(i-1);
                                String elementoS2Past = charactersString2.get(i-1);

                                if (elementoS1Past.equalsIgnoreCase(elementoS2) && elementoS1.equalsIgnoreCase(elementoS2Past)) {
                                    //switching letters, the score sould be increased by 1 (only 1 error
                                    score = score+1;
                                }

                            }
                        }
                    }
                }else{
                    System.out.print("logical error making the code in wordcorrector!");
                }

                score = score/charactersString1.size();//normalize
            }
        }

        if (differentSize) {
            int indice=0;
            for (int i=0; i < arrayCorto.size();i++) {
                String elementoS1 = arrayLargo.get(i);
                String elementoS2 = arrayCorto.get(i);

                if (elementoS1.equalsIgnoreCase(elementoS2)) {
                    score=score+1;
                }else{//paila, dio cero
                    boolean switched = false; //the error in the characters is initailized as it is not a switching error.
                    if (i+1 < arrayCorto.size()) {//if i-1 exists (ie not at the first letter).
                        String elementoS1Future = arrayLargo.get(i+1);
                        String elementoS2Future = arrayCorto.get(i+1);
                        if (elementoS1Future.equalsIgnoreCase(elementoS2)
                                && elementoS1.equalsIgnoreCase(elementoS2Future)) {
                            //switching letters, the score sould be increased by 1 (only 1 error
                            score = score+1;
                            i=i+1;// so it doesnt remove the next character.
                            switched = true;//it is a switching error.
                        }
                    }
                    if (!switched) {
                        //Remove and Repeat (R&R)
                        arrayLargo.remove(i);
                        indice = i;
                        i=i-1;//despues de esto no se puede volver a llamar al indice! por puede votar error
                    }
                }
                if (arrayLargo.size() == arrayCorto.size()) {
                    break;
                }
            }
            if (arrayLargo.size() == arrayCorto.size()) {//does conventional same size score detection.
                for (int i=indice; i < charactersString1.size() && i < charactersString2.size() ; i++) {
                    String elementoS1 = arrayLargo.get(i);
                    String elementoS2 = arrayCorto.get(i);
                    if (elementoS1.equalsIgnoreCase(elementoS2)) {
                        score=score+1;
                    }else{//if the elements are different.
                        if (0<i) {//if i-1 exists (ie not at the first letter).
                            String elementoS1Past = charactersString1.get(i-1);
                            String elementoS2Past = charactersString2.get(i-1);
                            if (elementoS1Past.equalsIgnoreCase(elementoS2)
                                    && elementoS1.equalsIgnoreCase(elementoS2Past)) {
                                //switching letters, the score sould be increased by 1 (only 1 error
                                score = score+1;
                            }

                        }
                    }
                }
            }
            int normalize;
            if (charactersString2.size() <= charactersString1.size()) {
                normalize = charactersString1.size();
            }else{
                normalize = charactersString2.size();
            }
            score = score/normalize;//normalize
        }
        return score;
    }

    public String getPeopleId() {
        return peopleId;
    }

    public void setPeopleId(String peopleId) {
        this.peopleId = peopleId;
    }

    public Map<String, List<String>> getRawData() {
        return rawData;
    }

    public void setRawData(Map<String, List<String>> rawData) {
        this.rawData = rawData;
    }
}
