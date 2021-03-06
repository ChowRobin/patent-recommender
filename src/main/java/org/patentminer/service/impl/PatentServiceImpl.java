package org.patentminer.service.impl;

import org.bson.types.ObjectId;
import org.patentminer.dao.PatentDao;
import org.patentminer.dao.PatentRepository;
import org.patentminer.exception.CheckException;
import org.patentminer.model.Patent;
import org.patentminer.model.PatentBO;
import org.patentminer.model.PatentDTO;
import org.patentminer.service.CompanyService;
import org.patentminer.service.InventorService;
import org.patentminer.service.PatentService;
import org.patentminer.util.CommonUtil;
import org.patentminer.util.MongoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.querydsl.QuerydslUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PatentServiceImpl implements PatentService {

    @Autowired
    private PatentRepository patentRepository;

    @Autowired
    private PatentDao patentDao;

    @Resource
    private MongoTemplate mongoTemplate;

    @Autowired
    private MongoUtil mongoUtil;

    @Autowired
    private CompanyService companyService;

    @Autowired
    private InventorService inventorService;

    @Override
    public Page<PatentDTO> listByCondition(Map<String, Object> parameterMap, int pageNo, int pageSize) {
        parameterMap.remove("pageNo");
        parameterMap.remove("pageSize");
        Query query = new Query();
        parameterMap.forEach((k, v)->{
            if (v instanceof Integer) {
                v = Integer.parseInt((String) v);
                query.addCriteria(Criteria.where(k).is(v));
            } else {
                query.addCriteria(Criteria.where(k).regex(".*?" + v + ".*"));
            }
        });
        return patentDao.paginationList(query, pageNo, pageSize);
    }

    @Override
    public String create(Patent patent) {
        return patentRepository.save(patent).getId();
    }

    private Update getUpdate(Patent patent) {
        Update update = new Update();
        String abstractStr, applicationDate, publicationDate;
        Integer topicId;
        if ((abstractStr = patent.getAbstractStr()) != null) {
            update.set("abstractStr", abstractStr);
        }
        if ((applicationDate = patent.getApplicationDate()) != null) {
            update.set("applicationDate", applicationDate);
        }
        if ((publicationDate = patent.getPublicationDate()) != null) {
            update.set("publicationDate", publicationDate);
        }
        if ((topicId = patent.getTopicId()) != null) {
            update.set("topicId", topicId);
        }
        return update;
    }

    @Override
    public String update(Patent patent, String id) {
        Query query = new Query(Criteria.where("id").is(new ObjectId(id)));
        if (mongoTemplate.findOne(query, Patent.class) == null) {
            throw new CheckException("This patent is not exists");
        } else {
            mongoTemplate.updateFirst(query, getUpdate(patent), Patent.class);
        }
        return id;
    }

    @Override
    public String delete(String id) {
        if (CommonUtil.unboxOptional(patentRepository.findById(id)) == null) {
            throw new CheckException("patent is not exists");
        } else {
            patentRepository.deleteById(id);
        }
        return id;
    }

    @Override
    public PatentDTO PO2DTO(Patent patent) {
        PatentDTO patentDTO = new PatentDTO(patent);
        patentDTO.setCompanies(patent.getCompanyIds()
                .stream()
                .map(id -> companyService.findById(id))
                .collect(Collectors.toList()));
        patentDTO.setInventors(patent.getInventorIds()
                .stream()
                .map(id -> inventorService.findById(id))
                .collect(Collectors.toList()));
        return patentDTO;
    }

    @Override
    public Patent DTO2PO(PatentDTO patentDTO) {
        return new Patent(patentDTO);
    }

    @Override
    public List<PatentDTO> listByCompanyId(String companyId) {
        Query query = new Query(Criteria.where("companyIds").is(new ObjectId(companyId)));
        return mongoTemplate.find(query, Patent.class).stream().map(p -> PO2DTO(p)).collect(Collectors.toList());
    }

    @Override
    public List<PatentDTO> listByInventorId(String inventorId) {
        Query query = new Query(Criteria.where("inventorIds").is(new ObjectId(inventorId)));
        return mongoTemplate.find(query, Patent.class).stream().map(p -> PO2DTO(p)).collect(Collectors.toList());
    }

    @Override
    public PatentBO findBOById(String id) {
        Query query = new Query(Criteria.where("id").is(new ObjectId(id)));
        return PO2BO(mongoTemplate.findOne(query, Patent.class));
    }

    public PatentBO PO2BO(Patent patent) {
        PatentBO patentBO = new PatentBO();
        patentBO.setId(patent.getId());
        patentBO.setInventionTitle(patent.getInventionTitle());
        patentBO.setInventionTitleCN(patent.getInventionTitleCN());
        return patentBO;
    }
}
