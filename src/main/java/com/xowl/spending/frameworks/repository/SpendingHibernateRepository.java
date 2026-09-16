package com.xowl.spending.frameworks.repository;

import com.xowl.spending.entities.Spending;
import com.xowl.spending.usecases.repository.SpendingRepository;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

public class SpendingHibernateRepository implements SpendingRepository {

    private final SessionFactoryProvider sessionFactoryProvider;

    public SpendingHibernateRepository() {
        this.sessionFactoryProvider = new SessionFactoryProvider();
    }

    public int getSpendingListSize() {
        SessionFactory sessionFactory = sessionFactoryProvider.provide();
        try (Session session = sessionFactory.openSession()) {
            Long count = session.createQuery("select count(s) from spending s where s.version = (select max(version) from spending)", Long.class)
                    .getSingleResult();
            return count == null ? 0 : count.intValue();
        }
    }

    public void saveSpendingList(List<Spending> spendingList) {
        SessionFactory sessionFactory = sessionFactoryProvider.provide();
        try (Session session = sessionFactory.openSession()) {
            try {
                Transaction tx = session.beginTransaction();

                Long version = session.createQuery("select coalesce(max(version), -1) from spending", Long.class).getSingleResult();
                Long versionSafe = version + 1;

                List<SpendingEntity> spendingEntities = spendingList.stream()
                        .map(spending -> spending.toEntity(versionSafe))
                        .collect(Collectors.toList());

                spendingEntities.forEach(session::save);

                tx.commit();
            } catch (RuntimeException e) {
                Transaction currentTx = session.getTransaction();
                if (currentTx != null) {
                    try { currentTx.rollback(); } catch (RuntimeException ignore) { }
                }
                throw e;
            }
        }
    }
}
