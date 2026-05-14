package com.matraknhash.service;

import com.matraknhash.dao.PartDao;
import com.matraknhash.model.Part;

import java.util.List;

public class PartService {
    private final PartDao dao;
    public PartService(PartDao dao) { this.dao = dao; }

    public List<Part> all()                       { return dao.findAll(); }
    public List<Part> search(String t)            { return (t == null || t.isBlank()) ? all() : dao.search(t.trim()); }
    public List<Part> lowStock()                  { return dao.findLowStock(); }
    public List<Part> lowStockForSeller(int s)    { return dao.findLowStockForSeller(s); }
    public Part save(Part p)                      { return p.getId() == 0 ? dao.insert(p) : (dao.update(p) ? p : p); }
    public boolean delete(int id)                 { return dao.delete(id); }
    public int countAll()                         { return dao.countAll(); }
    public int countBySeller(int s)               { return dao.countBySeller(s); }
}
