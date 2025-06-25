public class GenerateColumn {

    public static void defineCol(String name, DataType type) {

        /*
         * 
         *  We want to generate a class for each column that has the type baked in correctly
         * 
         *  e.g. Column('colName', 7, DataType.INTEGER, True, False)
         *       Column(name, value, type, nullable, primary) # constructor signature
         *
         *  public class tableColName{
         *      private String name;
         *      private Integer value;
         *      private boolean nullable;
         *      private boolean primary;
         * 
         *      public tableColName(String n, Integer v, boolean n, boolean p){
         *             name = n;
         *             value = v;
         *             nullable = n;
         *             primary = p;
         *      }
         * 
         *      public String getName(){return name;}
         *      public Integer getValue(){return value;}
         *      public boolean getNullable(){return nullable;}
         *      public boolean getPrimary){return primary;}
         *  }  
         * 
         */


        
    }   
}
