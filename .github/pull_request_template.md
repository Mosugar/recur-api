Closes #

## Modifications
<!-- Ce qui change et pourquoi, en quelques points. -->

## Vérifications
- [ ] `./mvnw verify` OK (ou `./mvnw test -Dtest="!RepositoryIntegrationTest"` sans Docker)
- [ ] Tests ajoutés ou mis à jour (service et/ou contrôleur)
- [ ] Changement de schéma : nouvelle migration Flyway (`src/main/resources/db/migration`), aucune migration existante modifiée
- [ ] Accès aux ressources limité à leur propriétaire (404 sinon)
- [ ] README / OpenAPI à jour si le contrat de l'API change
- [ ] Aucun secret dans le code, les logs ou les fichiers versionnés
