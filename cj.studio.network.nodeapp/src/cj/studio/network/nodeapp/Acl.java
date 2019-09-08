package cj.studio.network.nodeapp;

import java.util.ArrayList;
import java.util.List;

public class Acl {
    List<Ace> allows;
    List<Ace> denys;

    public Acl() {
        allows=new ArrayList<>();
        denys=new ArrayList<>();
    }

    public void add(Ace ace) {
        if(ace.getRight().equals("allow")){
            allows.add(ace);
            return;
        }
        if(ace.getRight().equals("deny")){
            denys.add(ace);
            return;
        }
    }
   public Ace deny(int index){
        return denys.get(index);
   }
   public int denyCount(){
        return denys.size();
   }
    public Ace allow(int index){
        return allows.get(index);
    }
    public int allowCount(){
        return allows.size();
    }
}
