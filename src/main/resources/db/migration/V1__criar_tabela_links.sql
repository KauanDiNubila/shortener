CREATE TABLE links (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(32) NOT NULL,
    url_original VARCHAR(2048) NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    expira_em TIMESTAMPTZ,
    cliques BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_links_codigo UNIQUE (codigo)
);

CREATE INDEX idx_links_expira_em ON links (expira_em) WHERE expira_em IS NOT NULL;
