package com.matraknhash.service;

import com.matraknhash.dao.SupplierDao;
import com.matraknhash.model.Supplier;

import java.util.List;

public class SupplierService {
    private final SupplierDao dao;
    public SupplierService(SupplierDao dao) { this.dao = dao; }

    public List<Supplier> all()          { return dao.findAll(); }
    public Supplier save(Supplier s)     { return s.getId() == 0 ? dao.insert(s) : (dao.update(s) ? s : s); }
    public boolean delete(int id)        { return dao.delete(id); }
}
