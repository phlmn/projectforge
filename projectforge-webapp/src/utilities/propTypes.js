import PropTypes from 'prop-types';

export const categoryItemPropType = PropTypes.shape({
    name: PropTypes.string,
    url: PropTypes.string,
});

export const categoryPropType = PropTypes.shape({
    name: PropTypes.string,
    items: PropTypes.arrayOf(categoryItemPropType),
});

export const buttonPropType = PropTypes.shape({
    style: PropTypes.oneOf(['primary', 'secondary', 'success', 'info', 'warning', 'danger', 'link']),
    title: PropTypes.string,
    id: PropTypes.string,
    handleClick: PropTypes.func,
    type: PropTypes.oneOf(['button', 'checkbox']),
    checked: PropTypes.bool,
});
