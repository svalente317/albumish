package albumish;

public class IntList {
    private int[] list;
    private int size;

    public IntList()
    {
        this.list = new int[4];
        this.size = 0;
    }

    public IntList(Integer[] orig) {
        this.list = new int[orig.length];
        this.size = orig.length;
        for (int idx = 0; idx < orig.length; idx++) {
            this.list[idx] = orig[idx];
        }
    }

    public void add(int value)
    {
        add(this.size, value);
    }

    public void add(int idx, int value)
    {
        int iii;
        if (this.size == this.list.length) {
            int[] newlist = new int[this.list.length * 2];
            for (iii = 0; iii < idx; iii++) {
                newlist[iii] = this.list[iii];
            }
            for (iii = this.size; iii > idx; iii--) {
                newlist[iii] = this.list[iii - 1];
            }
            this.list = newlist;
        }
        else {
            for (iii = this.size; iii > idx; iii--) {
                this.list[iii] = this.list[iii - 1];
            }
        }
        this.list[idx] = value;
        this.size++;
    }

    public int get(int idx)
    {
        return this.list[idx];
    }

    public void set(int idx, int value)
    {
        this.list[idx] = value;
    }

    public int size()
    {
        return this.size;
    }

    public void clear()
    {
        this.size = 0;
    }

    public int remove(int idx)
    {
        int retval = this.list[idx];
        this.size--;
        for (int iii = idx; iii < this.size; iii++) {
            this.list[iii] = this.list[iii + 1];
        }
        return retval;
    }

    public int[] finish() {
        if (this.list.length > this.size) {
            int[] trunc = new int[this.size];
            for (int idx = 0; idx < this.size; idx++) {
                trunc[idx] = this.list[idx];
            }
            this.list = trunc;
        }
        return this.list;
    }

    /**
     * @return the index of the first occurrence of the given value.
     */
    public int find(int value) {
        for (int idx = 0; idx < this.size; idx++) {
            if (this.list[idx] == value) {
                return idx;
            }
        }
        return -1;
    }

    public void swap(int idx, int other_idx) {
        int tmp = this.list[idx];
        this.list[idx] = this.list[other_idx];
        this.list[other_idx] = tmp;
    }

    public void truncate(int new_size) {
        if (this.size > new_size) {
            this.size = new_size;
        }
    }
}
