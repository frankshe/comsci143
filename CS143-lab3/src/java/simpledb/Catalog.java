package simpledb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Catalog keeps track of all available tables in the database and their
 * associated schemas.
 * For now, this is a stub catalog that must be populated with tables by a
 * user program before it can be used -- eventually, this should be converted
 * to a catalog that reads a catalog table from disk.
 * 
 * @Threadsafe
 */
public class Catalog {
    
    private class Table {
	private String pkeyField;
	private String name;
	private TupleDesc td;
	private DbFile file;
	
	public Table(DbFile file, String name, String pkeyField){
		this.td = file.getTupleDesc();
		this.name = name;
		this.pkeyField = pkeyField;
		this.file = file;
	}
	public String getName(){ return name; }
	public DbFile getFile(){ return file; }
	public String getKey(){ return pkeyField; }
    }
    private ArrayList<Table> tables;
    private ArrayList<TupleDesc> tds;
    private ArrayList<Integer> ids;
    private Map<Integer, Table> tableMap;
    /**
     * Constructor.
     * Creates a new, empty catalog.
     */
    public Catalog() {
	tables = new ArrayList<Table>(0);
	tds = new ArrayList<TupleDesc>(0);
	ids = new ArrayList<Integer>(0);
    }

    /**
     * Add a new table to the catalog.
     * This table's contents are stored in the specified DbFile.
     * @param file the contents of the table to add;  file.getId() is the identfier of
     *    this file/tupledesc param for the calls getTupleDesc and getFile
     * @param name the name of the table -- may be an empty string.  May not be null.  If a name
     * @param pkeyField the name of the primary key field
     * conflict exists, use the last table to be added as the table for a given name.
     */
    public void addTable(DbFile file, String name, String pkeyField) {
        // some code goes here
	//file.getTupleDesc() //get Tuple Desc
	if(name.equals(null))
		throw new IllegalArgumentException("name cannot be null");
	Integer tid = file.getId();
	for(int i = 0; i <tables.size(); i++)
	{
		if(tid == tables.get(i).getFile().getId())
		{
			tables.remove(i);
			tds.remove(i);
			ids.remove(i);
		}
	}
	tables.add(new Table(file, name, pkeyField));
        tds.add(file.getTupleDesc());
        ids.add(file.getId());

    }

    public void addTable(DbFile file, String name) {
        addTable(file, name, "");
    }

    /**
     * Add a new table to the catalog.
     * This table has tuples formatted using the specified TupleDesc and its
     * contents are stored in the specified DbFile.
     * @param file the contents of the table to add;  file.getId() is the identfier of
     *    this file/tupledesc param for the calls getTupleDesc and getFile
     */
    public void addTable(DbFile file) {
        addTable(file, (UUID.randomUUID()).toString());
    }

    /**
     * Return the id of the table with a specified name,
     * @throws NoSuchElementException if the table doesn't exist
     */
    public int getTableId(String name) throws NoSuchElementException {
	if ( name == null) //can a table have a null name?
		throw new NoSuchElementException();
	Iterator<Table> iter = this.tables.iterator();
	while ( iter.hasNext() )
	{
		Table temp = iter.next();
		if( temp.getName().equals(name))
			return temp.getFile().getId(); //dragon;
	}
        throw new NoSuchElementException();
    }

    /**
     * Returns the tuple descriptor (schema) of the specified table
     * @param tableid The id of the table, as specified by the DbFile.getId()
     *     function passed to addTable
     * @throws NoSuchElementException if the table doesn't exist
     */
    public TupleDesc getTupleDesc(int tableid) throws NoSuchElementException {
	return getDatabaseFile(tableid).getTupleDesc();
    }

    /**
     * Returns the DbFile that can be used to read the contents of the
     * specified table.
     * @param tableid The id of the table, as specified by the DbFile.getId()
     *     function passed to addTable
     */
    public DbFile getDatabaseFile(int tableid) throws NoSuchElementException {
        return getTable(tableid).getFile();
    }

    public Table getTable(int tableid){
	int tableLength = tables.size();
	for(int i = 0; i < tableLength; i++)
	{
		if(tableid == tables.get(i).getFile().getId())
			return tables.get(i);
	}
	throw new NoSuchElementException();
    }

    public String getPrimaryKey(int tableid) {
	return getTable(tableid).getKey();
    }

    public Iterator<Integer> tableIdIterator() {
	return ids.iterator();
    }

    public String getTableName(int id) {
	return getTable(id).getName();
    }
    
    /** Delete all tables from the catalog */
    public void clear() {
	tables.clear();
	tds.clear();
    }
    
    /**
     * Reads the schema from a file and creates the appropriate tables in the database.
     * @param catalogFile
     */
    public void loadSchema(String catalogFile) {
        String line = "";
        String baseFolder=new File(new File(catalogFile).getAbsolutePath()).getParent();
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(catalogFile)));
            
            while ((line = br.readLine()) != null) {
                //assume line is of the format name (field type, field type, ...)
                String name = line.substring(0, line.indexOf("(")).trim();
                //System.out.println("TABLE NAME: " + name);
                String fields = line.substring(line.indexOf("(") + 1, line.indexOf(")")).trim();
                String[] els = fields.split(",");
                ArrayList<String> names = new ArrayList<String>();
                ArrayList<Type> types = new ArrayList<Type>();
                String primaryKey = "";
                for (String e : els) {
                    String[] els2 = e.trim().split(" ");
                    names.add(els2[0].trim());
                    if (els2[1].trim().toLowerCase().equals("int"))
                        types.add(Type.INT_TYPE);
                    else if (els2[1].trim().toLowerCase().equals("string"))
                        types.add(Type.STRING_TYPE);
                    else {
                        System.out.println("Unknown type " + els2[1]);
                        System.exit(0);
                    }
                    if (els2.length == 3) {
                        if (els2[2].trim().equals("pk"))
                            primaryKey = els2[0].trim();
                        else {
                            System.out.println("Unknown annotation " + els2[2]);
                            System.exit(0);
                        }
                    }
                }
                Type[] typeAr = types.toArray(new Type[0]);
                String[] namesAr = names.toArray(new String[0]);
                TupleDesc t = new TupleDesc(typeAr, namesAr);
                HeapFile tabHf = new HeapFile(new File(baseFolder+"/"+name + ".dat"), t);
                addTable(tabHf,name,primaryKey);
                System.out.println("Added table : " + name + " with schema " + t);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        } catch (IndexOutOfBoundsException e) {
            System.out.println ("Invalid catalog entry : " + line);
            System.exit(0);
        }
    }
}

